package eu.egm.srv.cgm.importer.service;

import com.infra.InfrastructureUtils;
import com.infra.bpm.ProcessStartRequest;
import com.infra.event.EventPublisherService;
import eu.egm.data.cgm.dto.cgmes.CgmesConstants;
import eu.egm.data.cgm.dto.cgmes.CgmesProcess;
import eu.egm.data.cgm.dto.cgmes.CgmesRegion;
import eu.egm.data.cgm.dto.cgmes.EquipmentView;
import eu.egm.data.cgm.dto.cgmes.ImportFileStatus;
import eu.egm.data.cgm.dto.cgmes.ImportMetadata;
import eu.egm.data.cgm.dto.cgmes.ImportStatus;
import eu.egm.data.cgm.mapping.CgmesFileNameParser;
import eu.egm.srv.cgm.importer.domain.EquipmentDocument;
import eu.egm.srv.cgm.importer.domain.ImportStatusDocument;
import eu.egm.srv.cgm.importer.repository.EquipmentSearchRepository;
import eu.egm.srv.cgm.importer.repository.ImportStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CgmImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmImportService.class);
    public static final String STATE_INIT = "Init";
    public static final String STATE_STARTED = "Started";
    public static final String STATE_IN_PROGRESS = "In Progress";
    public static final String STATE_COMPLETE = "Complete";
    public static final String STATE_FAILED = "Failed";
    public static final String PROCESS_ID = "cgm-import";

    private final CgmesNetworkReader networkReader;
    private final EquipmentSearchRepository equipmentSearchRepository;
    private final ImportStatusRepository importStatusRepository;
    private final RawCgmesStorage rawCgmesStorage;
    private final EventPublisherService eventPublisher;
    private final InfrastructureUtils infrastructureUtils;

    public CgmImportService(CgmesNetworkReader networkReader,
                            EquipmentSearchRepository equipmentSearchRepository,
                            ImportStatusRepository importStatusRepository,
                            RawCgmesStorage rawCgmesStorage,
                            EventPublisherService eventPublisher,
                            InfrastructureUtils infrastructureUtils) {
        this.networkReader = networkReader;
        this.equipmentSearchRepository = equipmentSearchRepository;
        this.importStatusRepository = importStatusRepository;
        this.rawCgmesStorage = rawCgmesStorage;
        this.eventPublisher = eventPublisher;
        this.infrastructureUtils = infrastructureUtils;
    }

    public ImportStatus importCgmes(MultipartFile file) {
        return importCgmes(List.of(file), CgmesRegion.CORE, CgmesProcess.CGM);
    }

    public ImportStatus importCgmes(List<MultipartFile> files, CgmesRegion region, CgmesProcess process) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one CGMES file is required");
        }
        String networkId = UUID.randomUUID().toString();
        LOGGER.info("Storing {} CGMES file(s) for network {}", files.size(), networkId);
        List<StoredCgmesFile> storedFiles = storeFilesParallel(networkId, files, region, process);
        ImportStatus status = new ImportStatus(
                networkId,
                storedFiles.stream().map(StoredCgmesFile::fileName).toList().toString(),
                summarizeMetadata(storedFiles.stream().map(StoredCgmesFile::metadata).toList(), region, process),
                STATE_INIT,
                0,
                Instant.now(),
                "CGMES files stored; ready to start BPM import",
                null,
                storedFiles.stream()
                        .map(file -> new ImportFileStatus(file.objectId(), file.fileName(), STATE_INIT, STATE_STARTED, List.of(), "Stored in object storage", Instant.now()))
                        .toList());
        importStatusRepository.save(ImportStatusDocument.fromStatus(status));
        publishStoredObjects(networkId, storedFiles);
        return status;
    }

    public ImportStatus startCgmImport(ImportStatus requestedStatus) {
        ImportStatus status = requestedStatus.files().isEmpty() ? requireStatus(requestedStatus.networkId()) : requestedStatus;
        var instance = infrastructureUtils.businessProcessService().start(new ProcessStartRequest(
                PROCESS_ID,
                Map.of(
                        "networkId", status.networkId(),
                        "importStatus", status,
                        "objectIds", status.files().stream().map(ImportFileStatus::objectId).toList()),
                status.networkId()));
        ImportStatus updated = copy(status, STATE_IN_PROGRESS, status.indexedEquipmentCount(),
                "CGM import BPM process started", instance.processInstanceId(), status.files());
        importStatusRepository.save(ImportStatusDocument.fromStatus(updated));
        return updated;
    }

    public ImportStatus transformObject(String networkId, String objectId) {
        ImportStatus status = updateFile(networkId, objectId, STATE_STARTED, STATE_STARTED, List.of(), "CGMES transform started");
        ImportFileStatus fileStatus = fileStatus(status, objectId);
        try {
            byte[] bytes = rawCgmesStorage.read(objectId);
            List<EquipmentView> equipment = networkReader.read(networkId, status.metadata(), new ByteArrayInputStream(bytes));
            List<EquipmentDocument> documents = equipment.stream().map(EquipmentDocument::fromView).toList();
            equipmentSearchRepository.saveAll(documents);
            List<String> documentIds = documents.stream().map(EquipmentDocument::getDocumentId).toList();
            publishTransformedDocuments(networkId, objectId, documentIds);
            return updateFile(networkId, objectId, STATE_COMPLETE, STATE_STARTED, documentIds,
                    "CGMES transform completed for " + fileStatus.fileName());
        } catch (RuntimeException exception) {
            LOGGER.warn("CGMES transform failed for network {} object {}", networkId, objectId, exception);
            return updateFile(networkId, objectId, STATE_FAILED, STATE_FAILED, fileStatus.documentIds(),
                    "CGMES transform failed: " + exception.getMessage());
        }
    }

    public ImportStatus updateFileStatus(String networkId, String objectId, String status, String iidmStatus, List<String> documentIds, String message) {
        return updateFile(networkId, objectId, status, iidmStatus, documentIds, message);
    }

    public List<ImportStatus> listImports() {
        return importStatusRepository.findAll().stream()
                .map(ImportStatusDocument::toStatus)
                .toList();
    }

    public ImportStatus requireStatus(String networkId) {
        ImportStatusDocument document = importStatusRepository.findByNetworkId(networkId);
        if (document == null) {
            throw new IllegalArgumentException("Unknown CGM network id " + networkId);
        }
        return document.toStatus();
    }

    private ImportStatus updateFile(String networkId, String objectId, String fileState, String iidmState, List<String> documentIds, String message) {
        ImportStatus status = requireStatus(networkId);
        List<ImportFileStatus> files = status.files().stream()
                .map(file -> file.objectId().equals(objectId)
                        ? new ImportFileStatus(objectId, file.fileName(), fileState, iidmState,
                        documentIds == null || documentIds.isEmpty() ? file.documentIds() : documentIds,
                        message, Instant.now())
                        : file)
                .toList();
        ImportStatus updated = copy(status, aggregateState(files), documentCount(files), message, status.processInstanceId(), files);
        importStatusRepository.save(ImportStatusDocument.fromStatus(updated));
        return updated;
    }

    private List<StoredCgmesFile> storeFilesParallel(String networkId, List<MultipartFile> files, CgmesRegion region, CgmesProcess process) {
        ExecutorService executor = Executors.newFixedThreadPool(files.size());
        try {
            List<CompletableFuture<StoredCgmesFile>> tasks = new ArrayList<>();
            for (int index = 0; index < files.size(); index++) {
                int fileIndex = index;
                MultipartFile file = files.get(index);
                tasks.add(CompletableFuture.supplyAsync(() -> storeFile(networkId, fileIndex, file, region, process), executor));
            }
            return tasks.stream().map(CompletableFuture::join).toList();
        } finally {
            executor.shutdown();
        }
    }

    private StoredCgmesFile storeFile(String networkId, int index, MultipartFile file, CgmesRegion region, CgmesProcess process) {
        try {
            String originalName = fileName(file);
            ImportMetadata metadata = CgmesFileNameParser.parse(originalName, region, process);
            String objectId = objectId(networkId, index, originalName);
            rawCgmesStorage.store(objectId, file.getBytes(), file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            return new StoredCgmesFile(objectId, originalName, metadata);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read uploaded CGMES file", exception);
        }
    }

    private void publishStoredObjects(String networkId, List<StoredCgmesFile> storedFiles) {
        try {
            eventPublisher.publish(CgmesConstants.IMPORT_EXCHANGE, CgmesConstants.IMPORT_STORED_ROUTING_KEY,
                    new CgmImportStoredObjectsEvent(networkId, storedFiles.stream().map(StoredCgmesFile::objectId).toList(), Instant.now()));
        } catch (RuntimeException exception) {
            LOGGER.warn("CGMES import {} stored objects but import-start event publication failed", networkId, exception);
        }
    }

    private void publishTransformedDocuments(String networkId, String objectId, List<String> documentIds) {
        try {
            eventPublisher.publish(CgmesConstants.IMPORT_EXCHANGE, CgmesConstants.CGMES_TRANSFORMED_ROUTING_KEY,
                    new CgmesTransformedDocumentsEvent(networkId, objectId, documentIds, Instant.now()));
        } catch (RuntimeException exception) {
            LOGGER.warn("CGMES transform {} published documents but IIDM event publication failed", objectId, exception);
        }
    }

    private ImportFileStatus fileStatus(ImportStatus status, String objectId) {
        return status.files().stream()
                .filter(file -> file.objectId().equals(objectId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Object " + objectId + " is not linked to network " + status.networkId()));
    }

    private ImportStatus copy(ImportStatus source, String state, int documentCount, String message, String processInstanceId, List<ImportFileStatus> files) {
        return new ImportStatus(source.networkId(), source.fileName(), source.metadata(), state, documentCount,
                source.createdAt(), message, processInstanceId, files);
    }

    private String aggregateState(List<ImportFileStatus> files) {
        if (files.stream().anyMatch(file -> STATE_FAILED.equals(file.status()))) {
            return STATE_FAILED;
        }
        if (!files.isEmpty() && files.stream().allMatch(file -> STATE_COMPLETE.equals(file.status()))) {
            return STATE_COMPLETE;
        }
        if (files.stream().anyMatch(file -> STATE_STARTED.equals(file.status()) || STATE_COMPLETE.equals(file.status()))) {
            return STATE_IN_PROGRESS;
        }
        return STATE_INIT;
    }

    private int documentCount(List<ImportFileStatus> files) {
        return files.stream().mapToInt(file -> file.documentIds().size()).sum();
    }

    private String fileName(MultipartFile file) {
        return file.getOriginalFilename() == null || file.getOriginalFilename().isBlank() ? "network.xml" : file.getOriginalFilename();
    }

    private String objectId(String networkId, int index, String originalName) {
        return networkId + "/" + (index + 1) + "-" + Objects.requireNonNullElse(originalName, "network.xml");
    }

    private ImportMetadata summarizeMetadata(List<ImportMetadata> metadata, CgmesRegion region, CgmesProcess process) {
        if (metadata.isEmpty()) {
            return ImportMetadata.of(LocalDate.now(), "00:00", region, process);
        }
        ImportMetadata first = metadata.getFirst();
        return ImportMetadata.of(first.businessDay(), first.timestamp().toString(), region, process,
                join(metadata.stream().map(ImportMetadata::timeFrame).toList()),
                join(metadata.stream().map(ImportMetadata::tsoName).toList()),
                join(metadata.stream().map(ImportMetadata::cgmesProfileType).toList()),
                join(metadata.stream().map(ImportMetadata::versionNumber).toList()),
                join(metadata.stream().map(ImportMetadata::extension).toList()));
    }

    private String join(List<String> values) {
        Set<String> uniqueValues = new LinkedHashSet<>(values);
        uniqueValues.remove("");
        return String.join(",", uniqueValues);
    }

    private record StoredCgmesFile(String objectId, String fileName, ImportMetadata metadata) {
    }
}
