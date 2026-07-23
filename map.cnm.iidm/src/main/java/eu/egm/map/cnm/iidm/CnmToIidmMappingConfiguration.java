package eu.egm.map.cnm.iidm;

import eu.egm.mapping.MappingConfiguration;

/**
 * Mapping configuration for CNM-to-IIDM transformations.
 */
public class CnmToIidmMappingConfiguration extends MappingConfiguration {
    private final double nominalVoltage;
    private final String defaultSubstationId;
    private final String defaultSubstationName;
    private final String defaultVoltageLevelId;
    private final String defaultVoltageLevelName;
    private final String defaultBusId;
    private final String defaultBusName;
    private final double defaultLineX;

    public CnmToIidmMappingConfiguration() {
        this(400.0,
                "EGM_DEFAULT_SUBSTATION",
                "Default Substation",
                "EGM_DEFAULT_VL",
                "Default Voltage Level",
                "EGM_DEFAULT_BUS",
                "Default Bus",
                0.0001);
    }

    public CnmToIidmMappingConfiguration(
            double nominalVoltage,
            String defaultSubstationId,
            String defaultSubstationName,
            String defaultVoltageLevelId,
            String defaultVoltageLevelName,
            String defaultBusId,
            String defaultBusName,
            double defaultLineX) {
        this.nominalVoltage = nominalVoltage;
        this.defaultSubstationId = defaultSubstationId;
        this.defaultSubstationName = defaultSubstationName;
        this.defaultVoltageLevelId = defaultVoltageLevelId;
        this.defaultVoltageLevelName = defaultVoltageLevelName;
        this.defaultBusId = defaultBusId;
        this.defaultBusName = defaultBusName;
        this.defaultLineX = defaultLineX;
    }

    public double nominalVoltage() {
        return nominalVoltage;
    }

    public String defaultSubstationId() {
        return defaultSubstationId;
    }

    public String defaultSubstationName() {
        return defaultSubstationName;
    }

    public String defaultVoltageLevelId() {
        return defaultVoltageLevelId;
    }

    public String defaultVoltageLevelName() {
        return defaultVoltageLevelName;
    }

    public String defaultBusId() {
        return defaultBusId;
    }

    public String defaultBusName() {
        return defaultBusName;
    }

    public double defaultLineX() {
        return defaultLineX;
    }
}
