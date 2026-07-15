package eu.egm.data.common;

public record LoadFlowRequest(
        String requestId,
        NetworkCaseReference networkCase,
        boolean dcLoadFlow,
        boolean distributedSlack,
        String voltageInitMode) {
}
