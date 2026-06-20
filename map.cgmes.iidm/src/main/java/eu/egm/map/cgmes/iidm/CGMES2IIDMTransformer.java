package eu.egm.map.cgmes.iidm;

import eu.egm.com.data.cgmes.EquipmentView;
import eu.egm.com.data.iidm.IidmEquipment;
import eu.egm.com.data.iidm.IidmNetwork;
import eu.egm.com.mapping.MappingConfiguration;
import eu.egm.com.mapping.MappingService;
import eu.egm.com.mapping.transformer.Transformer;

import java.util.List;
import java.util.Objects;

/**
 * Transforms CGMES explorer DTOs into IIDM-oriented DTOs.
 */
public class CGMES2IIDMTransformer implements Transformer<IidmNetwork> {
    private final MappingService mappingService;
    private final CgmesIidmMappingConfiguration mappingConfiguration;

    public CGMES2IIDMTransformer(MappingService mappingService, CgmesIidmMappingConfiguration mappingConfiguration) {
        this.mappingService = Objects.requireNonNull(mappingService, "mappingService is required");
        this.mappingConfiguration = Objects.requireNonNull(mappingConfiguration, "mappingConfiguration is required");
    }

    @Override
    public MappingService mappingService() {
        return mappingService;
    }

    @Override
    public MappingConfiguration mappingConfiguration() {
        return mappingConfiguration;
    }

    public IidmEquipment transform(EquipmentView source) {
        return mappingService.map(source, IidmEquipment.class, mappingConfiguration.cgmesEquipmentToIidmEquipment());
    }

    public IidmNetwork transformNetwork(String networkId, List<EquipmentView> source) {
        List<IidmEquipment> equipment = source == null ? List.of() : source.stream()
                .map(this::transform)
                .toList();
        return new IidmNetwork(networkId, networkId, equipment);
    }
}
