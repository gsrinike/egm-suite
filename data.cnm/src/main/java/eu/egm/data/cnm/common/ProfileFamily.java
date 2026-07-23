package eu.egm.data.cnm.common;

/**
 * Top-level RDF profile family declared by the imported model.
 *
 * <p>Profile-specific values such as EQ, SSH, SV, TP, DL, GL, or NC profile
 * names belong to family-specific profile-kind enums. This enum deliberately
 * stays at family level so file metadata, RDF headers, Elasticsearch documents,
 * and downstream services do not mix profile kind and profile family concepts.</p>
 */
public enum ProfileFamily {
    NCP("NCP"),
    CGMES("CGMES"),
    Unknown("Unknown");

    private final String code;

    ProfileFamily(String code) {
        this.code = code;
    }

    public boolean equalsProfileFamily(String profileFamily) {
        return code.equalsIgnoreCase(profileFamily);
    }

    public static ProfileFamily fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Unknown;
        }
        String normalized = code.trim().replace('-', '_');
        if (CGMES.equalsProfileFamily(normalized)) {
            return CGMES;
        }
        if (NCP.equalsProfileFamily(normalized) || "NC".equalsIgnoreCase(normalized)) {
            return NCP;
        }
        if (isCgmesCoreProfile(normalized)) {
            return CGMES;
        }
        if (isNcOnlyProfile(normalized)) {
            return NCP;
        }
        return Unknown;
    }

    private static boolean isCgmesCoreProfile(String code) {
        return switch (code.toUpperCase()) {
            case "EQ", "SSH", "SV", "TP", "DL", "GL", "DY", "SC", "OP", "AP", "EQBD", "EQ_BD", "TPBD", "TP_BD",
                 "EQ_OP", "EQ_SC", "EQ_CO" -> true;
            default -> false;
        };
    }

    private static boolean isNcOnlyProfile(String code) {
        return switch (code.toUpperCase()) {
            case "AEAS", "ER", "GD", "IAM", "MA", "OR", "PS", "PSP", "RA", "RAS", "SAR", "SM", "SIS", "SHS", "SSI" -> true;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return code;
    }
}
