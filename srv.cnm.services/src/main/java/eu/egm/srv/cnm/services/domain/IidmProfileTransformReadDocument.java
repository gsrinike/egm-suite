package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.iidm.common.IidmTransformState;
import java.util.List;

/**
 * Read-only view of IIDM transform documents used to aggregate import file status.
 */
public record IidmProfileTransformReadDocument(
        String id,
        String importId,
        String fileId,
        List<String> sourceFileIds,
        List<String> sourceFileNames,
        String profileType,
        ProfileFamily profileFamily,
        String sourceProfilePayloadId,
        IidmTransformState transformState,
        String transformMessage,
        Object diagnostics,
        String iidmNetworkId,
        Object startedAt,
        Object completedAt,
        Object failedAt) {
    public IidmProfileTransformReadDocument {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        sourceFileNames = sourceFileNames == null ? List.of() : List.copyOf(sourceFileNames);
    }
}
