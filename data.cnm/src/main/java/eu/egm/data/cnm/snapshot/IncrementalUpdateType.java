package eu.egm.data.cnm.snapshot;

/**
 * Change type declared or inferred from an incremental .idm update.
 */
public enum IncrementalUpdateType {
    CREATE,
    UPDATE,
    DELETE,
    REPLACE,
    UNKNOWN
}
