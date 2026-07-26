package eu.egm.data.common.lfsa.sensitivity;

import java.util.List;

/**
 * Request to start a sensitivity-analysis run for a completed CNM import and selected IIDM networks.
 */
public record SensitivityAnalysisRunStartRequest(
        String fileImportId,
        List<String> iidmNetworkIds,
        String configurationId,
        String ptdfObjectId,
        String lodfObjectId,
        String glskObjectId) {
    public SensitivityAnalysisRunStartRequest {
        iidmNetworkIds = iidmNetworkIds == null ? List.of() : List.copyOf(iidmNetworkIds);
        ptdfObjectId = ptdfObjectId == null ? "" : ptdfObjectId;
        lodfObjectId = lodfObjectId == null ? "" : lodfObjectId;
        glskObjectId = glskObjectId == null ? "" : glskObjectId;
    }
}
