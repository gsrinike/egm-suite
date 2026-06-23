package eu.egm.srv.cgm.importer.domain;

import eu.egm.data.cgm.dto.cgmes.CgmesProcess;
import eu.egm.data.cgm.dto.cgmes.CgmesRegion;
import eu.egm.data.cgm.dto.cgmes.ImportFileStatus;
import eu.egm.data.cgm.dto.cgmes.ImportMetadata;
import eu.egm.data.cgm.dto.cgmes.ImportStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Storage document for one import batch. It backs the GUI network-id selector
 * and preserves upload history across application restarts.
 */
public class ImportStatusDocument {
    private String networkId;
    private String fileName;
    private String businessDay;
    private String timestamp;
    private CgmesRegion region;
    private CgmesProcess process;
    private String timeFrame;
    private String tsoName;
    private String cgmesProfileType;
    private String versionNumber;
    private String extension;
    private String state;
    private int indexedEquipmentCount;
    private Instant createdAt;
    private String message;
    private String processInstanceId;
    private List<ImportFileStatus> files;

    public ImportStatusDocument() {
    }

    public ImportStatusDocument(String networkId, String fileName, String businessDay, String timestamp,
                                CgmesRegion region, CgmesProcess process, String timeFrame, String tsoName,
                                String cgmesProfileType, String versionNumber, String extension, String state,
                                int indexedEquipmentCount, Instant createdAt, String message,
                                String processInstanceId, List<ImportFileStatus> files) {
        this.networkId = networkId;
        this.fileName = fileName;
        this.businessDay = businessDay;
        this.timestamp = timestamp;
        this.region = region;
        this.process = process;
        this.timeFrame = timeFrame;
        this.tsoName = tsoName;
        this.cgmesProfileType = cgmesProfileType;
        this.versionNumber = versionNumber;
        this.extension = extension;
        this.state = state;
        this.indexedEquipmentCount = indexedEquipmentCount;
        this.createdAt = createdAt;
        this.message = message;
        this.processInstanceId = processInstanceId;
        this.files = files;
    }

    public static ImportStatusDocument fromStatus(ImportStatus status) {
        return new ImportStatusDocument(
                status.networkId(),
                status.fileName(),
                status.metadata().businessDay().toString(),
                status.metadata().timestamp().toString(),
                status.metadata().region(),
                status.metadata().process(),
                status.metadata().timeFrame(),
                status.metadata().tsoName(),
                status.metadata().cgmesProfileType(),
                status.metadata().versionNumber(),
                status.metadata().extension(),
                status.state(),
                status.indexedEquipmentCount(),
                status.createdAt(),
                status.message(),
                status.processInstanceId(),
                status.files());
    }

    public ImportStatus toStatus() {
        ImportMetadata metadata = ImportMetadata.of(LocalDate.parse(businessDay), timestamp, region, process, timeFrame, tsoName, cgmesProfileType, versionNumber, extension);
        return new ImportStatus(networkId, fileName, metadata, state, indexedEquipmentCount, createdAt, message, processInstanceId, files);
    }

    public String getNetworkId() {
        return networkId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ImportFileStatus> getFiles() {
        return files;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }
}
