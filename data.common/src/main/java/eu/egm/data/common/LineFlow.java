package eu.egm.data.common;

public record LineFlow(
        String elementId,
        String fromNode,
        String toNode,
        double activePowerMw,
        double reactivePowerMvar,
        double loadingPercent) {
}
