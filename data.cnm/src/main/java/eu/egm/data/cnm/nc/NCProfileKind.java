package eu.egm.data.cnm.nc;

import java.util.Arrays;
import java.util.Locale;

/**
 * Network Code profile kinds used by coordinated regional operational
 * processes. The list is aligned with the ENTSO-E NC Profiles release 2.4.x
 * application profile set.
 */
public enum NCProfileKind {
    ASSESSED_ELEMENT_AVAILABILITY_SCHEDULE("AEAS", "Assessed Element Availability Schedule"),
    CONTINGENCY("CO", "Contingency"),
    EQUIPMENT_RELIABILITY("ER", "Equipment Reliability"),
    GRID_DISTURBANCE("GD", "Grid Disturbance"),
    IMPACT_ASSESSMENT_MATRIX("IAM", "Impact Assessment Matrix"),
    MONITORING_AREA("MA", "Monitoring Area"),
    OBJECT_REGISTRY("OR", "Object Registry"),
    POWER_SCHEDULE("PS", "Power Schedule"),
    POWER_SYSTEM_PROJECT("PSP", "Power System Project"),
    REMEDIAL_ACTION("RA", "Remedial Action"),
    REMEDIAL_ACTION_SCHEDULE("RAS", "Remedial Action Schedule"),
    SECURITY_ANALYSIS_RESULTS("SAR", "Security Analysis Results"),
    SENSITIVITY_MATRIX("SM", "Sensitivity Matrix"),
    STATE_INSTRUCTION_SCHEDULE("SIS", "State Instruction Schedule"),
    STEADY_STATE_HYPOTHESIS_SCHEDULE("SHS", "Steady State Hypothesis Schedule"),
    STEADY_STATE_INSTRUCTION("SSI", "Steady State Instruction"),
    UNKNOWN("UNKNOWN", "Unknown");

    private final String code;
    private final String label;

    NCProfileKind(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean matches(String value) {
        return normalize(code).equals(normalize(value)) || normalize(label).equals(normalize(value));
    }

    public static NCProfileKind fromCode(String value) {
        return Arrays.stream(values())
                .filter(kind -> kind.matches(value))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public static boolean isKnown(String value) {
        return fromCode(value) != UNKNOWN;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
