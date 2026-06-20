package eu.egm.com.data.iidm;

import java.util.Map;
import java.util.List;

/**
 * Searchable IIDM equipment projection.
 *
 * Additional source-specific values are kept in {@code attributes} so mapping
 * modules can carry information forward without widening this shared contract
 * for every CGMES or IIDM profile nuance.
 */
public record IidmEquipment(
        String id,
        String name,
        IidmEquipmentType type,
        String containerId,
        double nominalVoltage,
        List<IidmExtensionValue> extensions,
        Map<String, Object> attributes
) {
    public IidmEquipment {
        extensions = extensions == null ? List.of() : List.copyOf(extensions);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public IidmEquipment(String id, String name, IidmEquipmentType type, String containerId, double nominalVoltage, Map<String, Object> attributes) {
        this(id, name, type, containerId, nominalVoltage, List.of(), attributes);
    }
}
