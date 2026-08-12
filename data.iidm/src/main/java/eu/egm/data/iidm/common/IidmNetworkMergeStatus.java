package eu.egm.data.iidm.common;

import java.util.List;
import java.util.Map;

/**
 * Normal lifecycle message for one merged IIDM network artifact.
 */
public record IidmNetworkMergeStatus(
        String importId,
        String mergedNetworkId,
        List<String> iidmNetworkIds,
        List<CgmesIidmSourceFile> sourceFiles,
        IidmNetworkMergeState status,
        String businessDay,
        String businessTime,
        String timeFrame,
        String tsoName,
        CgmesIidmImportOptions importOptions,
        String occurredAt,
        String message) {
    public IidmNetworkMergeStatus(
            String importId,
            String mergedNetworkId,
            List<String> iidmNetworkIds,
            IidmNetworkMergeState status,
            String occurredAt,
            String message) {
        this(
                importId,
                mergedNetworkId,
                iidmNetworkIds,
                List.of(),
                status,
                "",
                "",
                "",
                "",
                new CgmesIidmImportOptions(Map.of()),
                occurredAt,
                message);
    }

    public IidmNetworkMergeStatus {
        mergedNetworkId = mergedNetworkId == null ? "" : mergedNetworkId;
        iidmNetworkIds = iidmNetworkIds == null ? List.of() : List.copyOf(iidmNetworkIds);
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
        businessDay = businessDay == null ? "" : businessDay;
        businessTime = businessTime == null ? "" : businessTime;
        timeFrame = timeFrame == null ? "" : timeFrame;
        tsoName = tsoName == null || tsoName.isBlank() ? "MERGED_CGM" : tsoName;
        importOptions = importOptions == null ? new CgmesIidmImportOptions(Map.of()) : importOptions;
        occurredAt = occurredAt == null ? "" : occurredAt;
        message = message == null ? "" : message;
    }
}
