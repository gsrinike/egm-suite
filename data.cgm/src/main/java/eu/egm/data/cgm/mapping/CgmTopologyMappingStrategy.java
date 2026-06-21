package eu.egm.data.cgm.mapping;

import eu.egm.data.cgm.dto.cgmes.*;
import eu.egm.data.cgm.dto.iidm.*;

import java.util.List;

public interface CgmTopologyMappingStrategy {
    boolean supports(CgmProfileGraph graph, ImportMetadata metadata);

    List<EquipmentView> project(String networkId, ImportMetadata metadata, CgmProfileGraph graph);
}
