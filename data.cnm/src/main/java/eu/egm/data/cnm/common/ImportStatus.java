package eu.egm.data.cnm.common;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate status for a network-model import.
 */
public record ImportStatus(
        String importId,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        ImportState state,
        List<ImportFileStatus> files,
        Instant createdAt,
        String message,
        IidmTransformationStatus iidmTransformationStatus) {
    public ImportStatus(
            String importId,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            ImportState state,
            List<ImportFileStatus> files,
            Instant createdAt,
            String message) {
        this(
                importId,
                serviceType,
                timeFrame,
                state,
                files,
                createdAt,
                message,
                IidmTransformationStatus.NOT_STARTED);
    }

    public ImportStatus {
        files = files == null ? List.of() : List.copyOf(files);
        iidmTransformationStatus = iidmTransformationStatus == null
                ? IidmTransformationStatus.NOT_STARTED
                : iidmTransformationStatus;
    }
}
