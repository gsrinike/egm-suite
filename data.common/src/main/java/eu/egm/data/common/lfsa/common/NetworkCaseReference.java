package eu.egm.data.common.lfsa.common;

public record NetworkCaseReference(
        String caseId,
        String networkId,
        String businessDay,
        String businessTime,
        TimeFrame timeFrame) {
}
