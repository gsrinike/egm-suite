package eu.egm.srv.iidm.transformer.api;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.iidm.common.IidmTransformState;
import java.util.List;

/**
 * Lightweight profile transform status for the IIDM menu.
 */
public record IidmTransformSummaryResponse(
        String transformId,
        String importId,
        String fileId,
        List<String> sourceFileIds,
        List<String> sourceFileNames,
        String profileType,
        ProfileFamily profileFamily,
        IidmTransformState transformState,
        String transformMessage,
        int diagnosticCount,
        String networkId,
        Object startedAt,
        Object completedAt,
        Object failedAt) {
}
