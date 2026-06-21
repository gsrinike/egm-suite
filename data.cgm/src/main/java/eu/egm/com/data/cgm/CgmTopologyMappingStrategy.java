package eu.egm.com.data.cgm;

import java.util.List;

public interface CgmTopologyMappingStrategy {
    boolean supports(CgmProfileGraph graph, ImportMetadata metadata);

    List<EquipmentView> project(String networkId, ImportMetadata metadata, CgmProfileGraph graph);
}
