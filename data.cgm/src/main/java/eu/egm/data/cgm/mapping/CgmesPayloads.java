package eu.egm.data.cgm.mapping;

import eu.egm.data.cgm.dto.cgmes.*;
import eu.egm.data.cgm.dto.iidm.*;

public final class CgmesPayloads {
    private static final int ZIP_MAGIC_1 = 0x50;
    private static final int ZIP_MAGIC_2 = 0x4B;

    private CgmesPayloads() {
    }

    public static boolean isZip(byte[] payload) {
        return payload.length >= 2
                && Byte.toUnsignedInt(payload[0]) == ZIP_MAGIC_1
                && Byte.toUnsignedInt(payload[1]) == ZIP_MAGIC_2;
    }
}
