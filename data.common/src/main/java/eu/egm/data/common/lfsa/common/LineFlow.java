package eu.egm.data.common.lfsa.common;

public record LineFlow(
        String elementId,
        String fromNode,
        String toNode,
        double activePowerMw,
        double reactivePowerMvar,
        double loadingPercent) {
}
