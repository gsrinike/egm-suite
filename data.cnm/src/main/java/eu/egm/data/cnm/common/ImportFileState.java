package eu.egm.data.cnm.common;

/**
 * Lifecycle state of one file within an import.
 */
public enum ImportFileState {
    INIT,
    STORED,
    PARSED,
    FAILED
}
