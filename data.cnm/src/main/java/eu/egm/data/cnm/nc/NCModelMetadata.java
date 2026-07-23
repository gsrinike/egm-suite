package eu.egm.data.cnm.nc;

/**
 * Metadata describing one Network Code profile payload.
 */
public record NCModelMetadata(
        NCProfileKind profileKind,
        String profileType,
        String version) {
}
