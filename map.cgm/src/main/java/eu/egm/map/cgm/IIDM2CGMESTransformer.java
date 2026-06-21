package eu.egm.map.cgm;

import eu.egm.data.cgm.dto.cgmes.EquipmentView;
import eu.egm.data.cgm.dto.cgmes.ImportMetadata;
import eu.egm.data.cgm.dto.iidm.IidmEquipment;
import eu.egm.mapping.MappingConfiguration;
import eu.egm.mapping.MappingService;
import eu.egm.mapping.transformer.Transformer;

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
