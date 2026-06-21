package eu.egm.data.cgm.mapping;

import eu.egm.data.cgm.dto.cgmes.*;
import eu.egm.data.cgm.dto.iidm.*;

public class BusBranchTopologyMappingStrategy extends AbstractTwoPassTopologyMappingStrategy {
    @Override
    public boolean supports(CgmProfileGraph graph, ImportMetadata metadata) {
        return true;
    }
}
