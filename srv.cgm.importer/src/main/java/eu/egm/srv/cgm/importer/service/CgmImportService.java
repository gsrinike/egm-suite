package eu.egm.srv.cgm.importer.service;

import com.infra.event.EventPublisherService;
import eu.egm.data.cgm.dto.cgmes.CgmesConstants;
import eu.egm.data.cgm.dto.cgmes.CgmesProcess;
import eu.egm.data.cgm.dto.cgmes.CgmesRegion;
import eu.egm.data.cgm.mapping.CgmesFileNameParser;
import eu.egm.data.cgm.dto.cgmes.EquipmentView;
import eu.egm.data.cgm.dto.cgmes.ImportMetadata;
import eu.egm.data.cgm.dto.cgmes.ImportStatus;
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
import java.util.Set;
import java.util.UUID;

@Service
public class CgmImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmImportService.class);
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
            List<EquipmentView> equipment = new ArrayList<>();
            List<ImportMetadata> importedMetadata = new ArrayList<>();
            for (MultipartFile file : files) {
                String originalName = fileName(file);
                // Each uploaded file can represent a different profile/TSO, so parse metadata per file.
                ImportMetadata metadata = CgmesFileNameParser.parse(originalName, region, process);
                importedMetadata.add(metadata);
                byte[] bytes = file.getBytes();
                rawCgmesStorage.store(networkId + "/" + originalName, bytes, file.getContentType() == null ? "application/octet-stream" : file.getContentType());
                equipment.addAll(networkReader.read(networkId, metadata, new ByteArrayInputStream(bytes)));
            }
            // Equipment from all uploaded profiles is indexed under one generated network id.
            equipmentSearchRepository.saveAll(equipment.stream().map(EquipmentDocument::fromView).toList());
            ImportStatus status = new ImportStatus(networkId, fileNames, summarizeMetadata(importedMetadata, region, process), "IMPORTED", equipment.size(), Instant.now(), "CGMES import completed");
            importStatusRepository.save(ImportStatusDocument.fromStatus(status));
            publishImportStatus(status);
            LOGGER.info("Completed CGMES import for network {} with {} indexed items", networkId, equipment.size());
            return status;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read uploaded CGMES file", exception);
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
}
