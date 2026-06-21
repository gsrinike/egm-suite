package eu.egm.data.cgm.dto.cgmes;

import com.powsybl.cgmes.model.CgmesSubset;

import java.util.Arrays;

/**
 * CGMES profile codes aligned with PowSyBl {@link CgmesSubset}.
 *
 * File names carry compact profile identifiers such as {@code EQ} or
 * {@code SSH}. The enum keeps those exchange codes connected to PowSyBl's
 * model subsets so services can avoid string-only profile handling.
 */
public enum CgmesProfileType {
    EQ("EQ", CgmesSubset.EQUIPMENT),
    TP("TP", CgmesSubset.TOPOLOGY),
    SV("SV", CgmesSubset.STATE_VARIABLES),
    SSH("SSH", CgmesSubset.STEADY_STATE_HYPOTHESIS),
    DY("DY", CgmesSubset.DYNAMIC),
    DL("DL", CgmesSubset.DIAGRAM_LAYOUT),
    GL("GL", CgmesSubset.GEOGRAPHICAL_LOCATION),
    EQ_BD("EQ_BD", CgmesSubset.EQUIPMENT_BOUNDARY),
    TP_BD("TP_BD", CgmesSubset.TOPOLOGY_BOUNDARY),
    UNKNOWN("UNKNOWN", CgmesSubset.UNKNOWN);

    private final String code;
    private final CgmesSubset powsyblSubset;

    CgmesProfileType(String code, CgmesSubset powsyblSubset) {
        this.code = code;
        this.powsyblSubset = powsyblSubset;
    }

    public String code() {
        return code;
    }

    public CgmesSubset powsyblSubset() {
        return powsyblSubset;
    }

    public static CgmesProfileType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(profile -> profile.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
