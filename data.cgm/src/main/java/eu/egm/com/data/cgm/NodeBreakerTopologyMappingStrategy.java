package eu.egm.com.data.cgm;

public class NodeBreakerTopologyMappingStrategy extends AbstractTwoPassTopologyMappingStrategy {
    @Override
    public boolean supports(CgmProfileGraph graph, ImportMetadata metadata) {
        return graph.containsType("ConnectivityNode") || graph.containsType("Terminal");
    }
}
