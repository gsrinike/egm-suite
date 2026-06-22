package eu.egm.srv.cgm.importer.service;

import com.infra.event.EventPublisherService;
import eu.egm.data.cgm.dto.cgmes.CgmesConstants;
import eu.egm.data.cgm.dto.cgmes.CgmesProcess;
import eu.egm.data.cgm.dto.cgmes.CgmesRegion;
import eu.egm.data.cgm.dto.cgmes.EquipmentView;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CgmImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmImportService.class);
    static final String STATE_IN_PROGRESS = "In Progress";
    static final String STATE_COMPLETE = "Complete";
    static final String STATE_FAILED = "Failed";

    private final CgmesNetworkReader networkReader;
    private final EquipmentSearchRepository equipmentSearchRepository;
    private final ImportStatusRepository importStatusRepository;
    private final RawCgmesStorage rawCgmesStorage;
    private final EventPublisherService eventPublisher;

    public CgmImportService(CgmesNetworkReader networkReader,
                            EquipmentSearchRepository equipmentSearchRepository,
                            ImportStatusRepository importStatusRepository,
                            RawCgmesStorage rawCgmesStorage,
                            EventPublisherService eventPublisher) {
        this.networkReader = networkReader;
        this.equipmentSearchRepository = equipmentSearchRepository;
        this.importStatusRepository = importStatusRepository;
        this.rawCgmesStorage = rawCgmesStorage;
        this.eventPublisher = eventPublisher;
    }

    public ImportStatus importCgmes(MultipartFile file) {
        return importCgmes(List.of(file), CgmesRegion.CORE, CgmesProcess.CGM);
    }

    public ImportStatus importCgmes(List<MultipartFile> files, CgmesRegion region, CgmesProcess process) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one CGMES file is required");
        }
        String networkId = UUID.randomUUID().toString();
        String fileNames = files.stream().map(this::fileName).toList().toString();
        LOGGER.info("Starting CGMES import for network {} from files {}", networkId, fileNames);
        try {
            List<StoredCgmesFile> storedFiles = storeFiles(networkId, files, region, process);
            ImportStatus status = new ImportStatus(
                    networkId,
                    fileNames,
                    summarizeMetadata(storedFiles.stream().map(StoredCgmesFile::metadata).toList(), region, process),
                    STATE_IN_PROGRESS,
                    0,
                    Instant.now(),
                    "CGMES files stored; background processing started");
            importStatusRepository.save(ImportStatusDocument.fromStatus(status));
            publishStoredObjects(networkId, storedFiles);
            processStoredFilesAsync(networkId, fileNames, status.metadata(), storedFiles);
            return status;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read uploaded CGMES file", exception);
        }
    }

    private List<StoredCgmesFile> storeFiles(String networkId, List<MultipartFile> files, CgmesRegion region, CgmesProcess process) throws IOException {
        List<StoredCgmesFile> storedFiles = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            String originalName = fileName(file);
            // Each uploaded file can represent a different profile/TSO, so parse metadata per file.
            ImportMetadata metadata = CgmesFileNameParser.parse(originalName, region, process);
            byte[] bytes = file.getBytes();
            String objectId = objectId(networkId, index, originalName);
            rawCgmesStorage.store(objectId, bytes, file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            storedFiles.add(new StoredCgmesFile(objectId, metadata, bytes));
        }
        return storedFiles;
    }

    private void processStoredFilesAsync(String networkId, String fileNames, ImportMetadata statusMetadata, List<StoredCgmesFile> storedFiles) {
        ExecutorService executor = Executors.newFixedThreadPool(storedFiles.size());
        List<EquipmentView> indexedEquipment = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> tasks = storedFiles.stream()
                .map(file -> CompletableFuture.runAsync(() -> processStoredFile(networkId, file, indexedEquipment), executor)
                        .exceptionally(exception -> {
                            failures.add(exception);
                            LOGGER.warn("CGMES background processing failed for network {} object {}", networkId, file.objectId(), exception);
                            return null;
                        }))
                .toList();
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .whenComplete((ignored, exception) -> {
                    try {
                        if (exception != null) {
                            failures.add(exception);
                        }
                        ImportStatus finalStatus = failures.isEmpty()
                                ? new ImportStatus(networkId, fileNames, statusMetadata, STATE_COMPLETE, indexedEquipment.size(), Instant.now(), "CGMES import completed")
                                : new ImportStatus(networkId, fileNames, statusMetadata, STATE_FAILED, indexedEquipment.size(), Instant.now(), "CGMES import failed for " + failures.size() + " file(s)");
                        importStatusRepository.save(ImportStatusDocument.fromStatus(finalStatus));
                        publishImportStatus(finalStatus);
                        LOGGER.info("Finished background CGMES import for network {} with state {} and {} indexed items",
                                networkId, finalStatus.state(), finalStatus.indexedEquipmentCount());
                    } finally {
                        executor.shutdown();
                    }
                });
    }

    private void processStoredFile(String networkId, StoredCgmesFile file, List<EquipmentView> indexedEquipment) {
        List<EquipmentView> equipment = networkReader.read(networkId, file.metadata(), new ByteArrayInputStream(file.bytes()));
        equipmentSearchRepository.saveAll(equipment.stream().map(EquipmentDocument::fromView).toList());
        indexedEquipment.addAll(equipment);
    }

    private void publishStoredObjects(String networkId, List<StoredCgmesFile> storedFiles) {
        try {
            eventPublisher.publish(CgmesConstants.IMPORT_EXCHANGE, CgmesConstants.IMPORT_STORED_ROUTING_KEY,
                    new CgmImportStoredObjectsEvent(networkId, storedFiles.stream().map(StoredCgmesFile::objectId).toList(), Instant.now()));
        } catch (RuntimeException exception) {
            // Storage and status persistence are complete; keep the batch visible even if external event publishing fails.
            LOGGER.warn("CGMES import {} stored objects but import-start event publication failed", networkId, exception);
        }
    }

    private void publishImportStatus(ImportStatus status) {
        try {
            eventPublisher.publish(CgmesConstants.IMPORT_EXCHANGE, CgmesConstants.IMPORTED_ROUTING_KEY, status);
        } catch (RuntimeException exception) {
            // Import is considered successful once storage/indexing succeeds; event publishing is best effort.
            LOGGER.warn("CGMES import {} completed but import event publication failed", status.networkId(), exception);
        }
    }

    public List<ImportStatus> listImports() {
        return importStatusRepository.findAll().stream()
                .map(ImportStatusDocument::toStatus)
                .toList();
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
        // Status documents represent an upload batch, so multi-file metadata is summarized as comma-separated unique values.
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

    private record StoredCgmesFile(String objectId, ImportMetadata metadata, byte[] bytes) {
    }
}
