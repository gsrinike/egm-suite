package eu.egm.data.common;

public record RemedialAction(
        String actionId,
        String assetId,
        String actionType,
        String beforeValue,
        String afterValue,
        double expectedImpactMw,
        String validationStatus) {
}
