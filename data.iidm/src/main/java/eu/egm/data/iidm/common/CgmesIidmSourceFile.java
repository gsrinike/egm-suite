package eu.egm.data.iidm.common;

import eu.egm.data.cnm.common.ProfileFamily;

/**
 * Raw CGMES source file descriptor used by the direct PowSyBl IIDM importer.
 */
public record CgmesIidmSourceFile(
        String fileId,
        String fileName,
        String objectId,
        ProfileFamily profileFamily,
        String profileType) {
    public CgmesIidmSourceFile {
        fileId = valueOr(fileId);
        fileName = valueOr(fileName);
        objectId = valueOr(objectId);
        profileFamily = profileFamily == null ? ProfileFamily.Unknown : profileFamily;
        profileType = valueOr(profileType);
    }

    private static String valueOr(String value) {
        return value == null ? "" : value;
    }
}
