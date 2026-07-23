package eu.egm.map.cnm.iidm;

import eu.egm.mapping.MappingConfiguration;
import java.util.Map;

/**
 * Mapping configuration for direct PowSyBl CGMES source-file import.
 */
public class CgmesSourceToIidmMappingConfiguration extends MappingConfiguration {
    private final Map<String, String> importProperties;

    public CgmesSourceToIidmMappingConfiguration(Map<String, String> importProperties) {
        this.importProperties = importProperties == null ? Map.of() : Map.copyOf(importProperties);
    }

    public Map<String, String> importProperties() {
        return importProperties;
    }
}
