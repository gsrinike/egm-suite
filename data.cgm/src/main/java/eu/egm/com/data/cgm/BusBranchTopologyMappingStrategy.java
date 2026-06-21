package eu.egm.com.data.cgm;

public class BusBranchTopologyMappingStrategy extends AbstractTwoPassTopologyMappingStrategy {
    @Override
    public boolean supports(CgmProfileGraph graph, ImportMetadata metadata) {
        return true;
    }
}
