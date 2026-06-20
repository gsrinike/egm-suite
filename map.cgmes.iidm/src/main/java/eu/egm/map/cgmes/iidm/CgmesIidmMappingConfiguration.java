package eu.egm.map.cgmes.iidm;

import eu.egm.com.data.cgmes.EquipmentView;
import eu.egm.com.data.iidm.IidmEquipment;
import eu.egm.com.mapping.FieldMapping;
import eu.egm.com.mapping.MappingDefinition;

import java.util.List;
import java.util.Map;

/**
 * Central mapping definitions for CGMES and IIDM DTO projections.
 *
 * Keeping these definitions together makes the transformer behavior explicit
 * and ready for externalization to XML/YAML configuration later.
 */
public final class CgmesIidmMappingConfiguration {
    private static final Map<String, String> CGMES_TO_IIDM_TYPES = Map.ofEntries(
            Map.entry("SUBSTATION", "SUBSTATION"),
            Map.entry("VOLTAGE_LEVEL", "VOLTAGE_LEVEL"),
            Map.entry("BUS", "BUS"),
            Map.entry("LINE", "LINE"),
            Map.entry("TRANSFORMER", "TWO_WINDINGS_TRANSFORMER"),
            Map.entry("GENERATOR", "GENERATOR"),
            Map.entry("LOAD", "LOAD"),
            Map.entry("SHUNT", "SHUNT_COMPENSATOR"),
            Map.entry("SWITCH", "SWITCH"),
            Map.entry("STATE_VARIABLE", "STATE_VARIABLE"),
            Map.entry("UNKNOWN", "UNKNOWN")
    );
    private static final Map<String, String> IIDM_TO_CGMES_TYPES = Map.ofEntries(
            Map.entry("SUBSTATION", "SUBSTATION"),
            Map.entry("VOLTAGE_LEVEL", "VOLTAGE_LEVEL"),
            Map.entry("BUS", "BUS"),
            Map.entry("LINE", "LINE"),
            Map.entry("TWO_WINDINGS_TRANSFORMER", "TRANSFORMER"),
            Map.entry("GENERATOR", "GENERATOR"),
            Map.entry("LOAD", "LOAD"),
            Map.entry("SHUNT_COMPENSATOR", "SHUNT"),
            Map.entry("SWITCH", "SWITCH"),
            Map.entry("STATE_VARIABLE", "STATE_VARIABLE"),
            Map.entry("UNKNOWN", "UNKNOWN")
    );

    private CgmesIidmMappingConfiguration() {
    }

    public static MappingDefinition cgmesEquipmentToIidmEquipment() {
        return new MappingDefinition(
                "cgmes-equipment-to-iidm-equipment",
                EquipmentView.class,
                IidmEquipment.class,
                List.of(
                        FieldMapping.of("id", "id"),
                        FieldMapping.of("name", "name"),
                        FieldMapping.of("type", "type", CGMES_TO_IIDM_TYPES),
                        FieldMapping.of("containerId", "containerId"),
                        FieldMapping.of("nominalVoltage", "nominalVoltage"),
                        FieldMapping.of("attributes", "attributes")
                )
        );
    }

    public static MappingDefinition iidmEquipmentToCgmesEquipment() {
        return new MappingDefinition(
                "iidm-equipment-to-cgmes-equipment",
                IidmEquipment.class,
                EquipmentView.class,
                List.of(
                        FieldMapping.of("id", "id"),
                        FieldMapping.of("name", "name"),
                        FieldMapping.of("type", "type", IIDM_TO_CGMES_TYPES),
                        FieldMapping.of("containerId", "containerId"),
                        FieldMapping.of("nominalVoltage", "nominalVoltage"),
                        FieldMapping.of("attributes", "attributes")
                )
        );
    }
}
