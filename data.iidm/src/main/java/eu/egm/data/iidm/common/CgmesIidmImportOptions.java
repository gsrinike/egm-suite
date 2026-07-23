package eu.egm.data.iidm.common;

import java.util.Map;

/**
 * PowSyBl CGMES import option bag passed with direct source transform requests.
 */
public record CgmesIidmImportOptions(
        Map<String, String> properties) {
    public CgmesIidmImportOptions {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }
}
