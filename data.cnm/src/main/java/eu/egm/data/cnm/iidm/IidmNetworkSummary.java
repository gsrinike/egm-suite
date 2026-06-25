package eu.egm.data.cnm.iidm;

import java.util.List;

/**
 * Lightweight IIDM network projection inspired by PowSyBl network concepts.
 */
public record IidmNetworkSummary(
        String networkId,
        String name,
        String sourceFormat,
        List<IidmElementSummary> elements) {
    public IidmNetworkSummary {
        elements = elements == null ? List.of() : List.copyOf(elements);
    }
}
