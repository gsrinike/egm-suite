package eu.egm.data.cnm.cgmes;

import java.util.List;

/**
 * Topology profile DTO containing topology nodes and connectivity associations.
 */
public record CgmesTopologyProfile(
        List<CgmesProfileEntity> topologyNodes,
        List<CgmesProfileEntity> connectivityNodes,
        List<CgmesProfileEntity> terminalAssociations) {
    public CgmesTopologyProfile {
        topologyNodes = topologyNodes == null ? List.of() : List.copyOf(topologyNodes);
        connectivityNodes = connectivityNodes == null ? List.of() : List.copyOf(connectivityNodes);
        terminalAssociations = terminalAssociations == null ? List.of() : List.copyOf(terminalAssociations);
    }
}
