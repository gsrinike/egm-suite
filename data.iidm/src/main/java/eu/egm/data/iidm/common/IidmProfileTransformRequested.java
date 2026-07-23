package eu.egm.data.iidm.common;

import eu.egm.data.cnm.common.ProfileFamily;
import java.util.List;
import java.util.Map;

/**
 * Event emitted after a CNM profile payload has been parsed and persisted.
 */
public record IidmProfileTransformRequested(
        String importId,
        String fileId,
        String sourceProfilePayloadId,
        String sourceSnapshotId,
        String profileType,
        ProfileFamily profileFamily,
        String objectId,
        String businessDay,
        String businessTime,
        String tsoName,
        String timeFrame,
        List<CgmesIidmSourceFile> sourceFiles,
        CgmesIidmImportOptions importOptions) {
    public IidmProfileTransformRequested(
            String importId,
            String fileId,
            String sourceProfilePayloadId,
            String sourceSnapshotId,
            String profileType,
            ProfileFamily profileFamily,
            String objectId,
            String businessDay,
            String businessTime,
            String tsoName,
            String timeFrame) {
        this(
                importId,
                fileId,
                sourceProfilePayloadId,
                sourceSnapshotId,
                profileType,
                profileFamily,
                objectId,
                businessDay,
                businessTime,
                tsoName,
                timeFrame,
                sourceFile(fileId, objectId, profileFamily, profileType),
                new CgmesIidmImportOptions(Map.of()));
    }

    public IidmProfileTransformRequested {
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
        importOptions = importOptions == null ? new CgmesIidmImportOptions(Map.of()) : importOptions;
    }

    private static List<CgmesIidmSourceFile> sourceFile(
            String fileId,
            String objectId,
            ProfileFamily profileFamily,
            String profileType) {
        if (objectId == null || objectId.isBlank()) {
            return List.of();
        }
        return List.of(new CgmesIidmSourceFile(fileId, "", objectId, profileFamily, profileType));
    }
}
