package eu.egm.srv.iidm.transformer.domain;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.iidm.common.IidmDiagnostic;
import eu.egm.data.iidm.common.IidmTransformState;
import java.util.List;

/**
 * Transform status document owned by the IIDM transformer service.
 */
public record IidmProfileTransformDocument(
        String id,
        String importId,
        String fileId,
        String profileType,
        ProfileFamily profileFamily,
        String sourceProfilePayloadId,
        IidmTransformState transformState,
        String transformMessage,
        List<IidmDiagnostic> diagnostics,
        String iidmNetworkId,
        Object startedAt,
        Object completedAt,
        Object failedAt) {
    public IidmProfileTransformDocument {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
