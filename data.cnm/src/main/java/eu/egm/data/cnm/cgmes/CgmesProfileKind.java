package eu.egm.data.cnm.cgmes;

import java.util.Arrays;
import java.util.Locale;

/**
 * CGMES profile kinds used during import classification and profile-specific
 * DTO extraction.
 */
public enum CgmesProfileKind {
    EQUIPMENT("EQ", "Equipment"),
    TOPOLOGY("TP", "Topology"),
    STEADY_STATE_HYPOTHESIS("SSH", "Steady State Hypothesis"),
    STATE_VARIABLES("SV", "State Variables"),
    DIAGRAM_LAYOUT("DL", "Diagram Layout"),
    GEOGRAPHICAL_LOCATION("GL", "Geographical Location"),
    DYNAMICS("DY", "Dynamics"),
    SHORT_CIRCUIT("SC", "Short Circuit"),
    OPERATION("OP", "Operation"),
    AVAILABILITY_PLAN("AP", "Availability Plan"),
    BOUNDARY_EQUIPMENT("EQ_BD", "Boundary Equipment"),
    EQUIPMENT_OPERATION("EQ_OP", "Equipment Operation"),
    EQUIPMENT_SHORT_CIRCUIT("EQ_SC", "Equipment Short Circuit"),
    EQUIPMENT_CONTINGENCY("EQ_CO", "Equipment Contingency"),
    CONTINGENCY("CO", "Contingency"),
    MANIFEST("MF", "Manifest"),
    UNKNOWN("UNKNOWN", "Unknown");

    private final String code;
    private final String label;

    CgmesProfileKind(String code, String label) {
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

    public static CgmesProfileKind fromCode(String value) {
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
