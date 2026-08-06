package eu.egm.data.cnm.common;

/**
 * Lifecycle state of an import request or individual imported file.
 */
public enum ImportState {
    INIT,
    STARTED,
    INIT_TRANSFORMATION,
    STORED,
    RDF_EXTRACTED,
    SUCCESS,
    FAILED
}
