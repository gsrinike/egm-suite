package eu.egm.data.cnm.common;

import java.util.Arrays;

/**
 * RDF profile family declared by the imported model.
 */
public enum ProfileFamily {
    EQ("EQ"),
    DL("DL"),
    DY("DY"),
    EB("EB"),
    GL("GL"),
    OP("OP"),
    SC("SC"),
    SV("SV"),
    SSH("SSH"),
    TP("TP"),
    NCP("NCP"),
    CGMES("CGMES"),
    Unknown("UK");

    private final String code;

    ProfileFamily(String code) {
        this.code = code;
    }

    public boolean equalsProfileFamily(String profileFamily) {
        return code.equalsIgnoreCase(profileFamily);
    }

    public static ProfileFamily fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.equalsProfileFamily(code))
                .findFirst()
                .orElse(Unknown);
    }

    @Override
    public String toString() {
        return code;
    }
}
