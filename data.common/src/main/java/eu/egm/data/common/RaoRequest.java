package eu.egm.data.common;

public record RaoRequest(
        String requestId,
        NetworkCaseReference networkCase,
        SecurityAnalysisResult securityAnalysisResult,
        double loadingThreshold,
        int maxPstActions,
        int maxRedispatchActions) {
}
