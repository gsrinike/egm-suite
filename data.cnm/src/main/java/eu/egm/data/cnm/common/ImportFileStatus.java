package eu.egm.data.cnm.common;

import java.time.Instant;
import java.util.List;

/**
 * Status and metadata for one uploaded RDF payload.
 */
public record ImportFileStatus(
        String fileId,
        String fileName,
        String objectId,
        ImportFileState state,
        ProfileFamily profileFamily,
        String businessDay,
        String businessTime,
        String modelTimeFrame,
        String tsoName,
        String profileType,
        String modelVersion,
        List<RdfProfileReference> profiles,
        String message,
        IidmTransformationStatus iidmTransformationStatus,
        int iidmTransformationCount,
        int iidmTransformationCompletedCount,
        int iidmTransformationFailedCount,
        Instant uploadedAt) {
    public ImportFileStatus {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
        iidmTransformationStatus = iidmTransformationStatus == null
                ? IidmTransformationStatus.NOT_STARTED
                : iidmTransformationStatus;
        iidmTransformationCount = Math.max(iidmTransformationCount, 0);
        iidmTransformationCompletedCount = Math.max(iidmTransformationCompletedCount, 0);
        iidmTransformationFailedCount = Math.max(iidmTransformationFailedCount, 0);
    }
}
