package eu.egm.map.cgmes.iidm;

import eu.egm.com.data.cgmes.EquipmentView;
import eu.egm.com.data.cgmes.ImportMetadata;
import eu.egm.com.data.iidm.IidmEquipment;
import eu.egm.com.mapping.MappingService;

import java.util.Map;

/**
 * Transforms IIDM-oriented DTOs into CGMES explorer DTOs.
 */
public class IIDM2CGMESTransformer {
    private final MappingService mappingService;

    public IIDM2CGMESTransformer(MappingService mappingService) {
        this.mappingService = mappingService;
    }

    public EquipmentView transform(IidmEquipment source, String networkId, ImportMetadata metadata) {
        EquipmentView mapped = mappingService.map(source, EquipmentView.class, CgmesIidmMappingConfiguration.iidmEquipmentToCgmesEquipment());
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
