package eu.egm.data.common.lfsa.common;

/**
 * Application-level orchestration choice for the load-flow step before security analysis.
 */
public enum LoadFlowStrategy {
    DC_ONLY,
    AC_ONLY,
    AC_WITH_DC_FAILOVER
}
