package eu.egm.map.cgmes.iidm;

import eu.egm.com.data.cgmes.EquipmentView;
import eu.egm.com.data.cgmes.ImportMetadata;
import eu.egm.com.data.iidm.IidmEquipment;
import eu.egm.com.mapping.MappingConfiguration;
import eu.egm.com.mapping.MappingService;
import eu.egm.com.mapping.transformer.Transformer;

import java.util.Map;
import java.util.Objects;

/**
 * Transforms IIDM-oriented DTOs into CGMES explorer DTOs.
 */
public class IIDM2CGMESTransformer implements Transformer<EquipmentView> {
    private final MappingService mappingService;
    private final CgmesIidmMappingConfiguration mappingConfiguration;

    public IIDM2CGMESTransformer(MappingService mappingService, CgmesIidmMappingConfiguration mappingConfiguration) {
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

    public EquipmentView transform(IidmEquipment source, String networkId, ImportMetadata metadata) {
        EquipmentView mapped = mappingService.map(source, EquipmentView.class, mappingConfiguration.iidmEquipmentToCgmesEquipment());
        return new EquipmentView(
                mapped.id(),
                networkId,
                metadata,
                mapped.name(),
                mapped.type(),
                mapped.containerId(),
                mapped.nominalVoltage(),
                mapped.attributes() == null ? Map.of() : mapped.attributes()
        );
    }
}
