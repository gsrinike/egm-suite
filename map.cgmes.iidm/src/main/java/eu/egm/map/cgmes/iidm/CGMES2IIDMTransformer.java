package eu.egm.map.cgmes.iidm;

import eu.egm.com.data.cgmes.EquipmentView;
import eu.egm.com.data.iidm.IidmEquipment;
import eu.egm.com.data.iidm.IidmNetwork;
import eu.egm.com.mapping.MappingService;

import java.util.List;

/**
 * Transforms CGMES explorer DTOs into IIDM-oriented DTOs.
 */
public class CGMES2IIDMTransformer implements Transformer<IidmNetwork> {
    private final MappingService mappingService;

    public CGMES2IIDMTransformer(MappingService mappingService) {
        this.mappingService = mappingService;
    }

    public IidmEquipment transform(EquipmentView source) {
        return mappingService.map(source, IidmEquipment.class, CgmesIidmMappingConfiguration.cgmesEquipmentToIidmEquipment());
    }

    public IidmNetwork transformNetwork(String networkId, List<EquipmentView> source) {
        List<IidmEquipment> equipment = source == null ? List.of() : source.stream()
                .map(this::transform)
                .toList();
        return new IidmNetwork(networkId, networkId, equipment);
    }
}
