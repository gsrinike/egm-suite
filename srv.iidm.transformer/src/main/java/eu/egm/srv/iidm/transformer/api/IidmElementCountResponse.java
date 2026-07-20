package eu.egm.srv.iidm.transformer.api;

/**
 * Count of one IIDM element family in a transformed network.
 */
public record IidmElementCountResponse(String elementType, long count) {
}
