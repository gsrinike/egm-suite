package eu.egm.com.data.cgm;

import java.util.Map;

public record EquipmentView(
        String id,
        String networkId,
        ImportMetadata metadata,
        String name,
        EquipmentType type,
        String containerId,
        double nominalVoltage,
        Map<String, Object> attributes
) {
}
