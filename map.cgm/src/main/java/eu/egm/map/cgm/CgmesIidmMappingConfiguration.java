package eu.egm.map.cgm;

import eu.egm.data.cgm.dto.cgmes.EquipmentView;
import eu.egm.data.cgm.dto.iidm.IidmEquipment;
import eu.egm.mapping.FieldMapping;
import eu.egm.mapping.MappingConfiguration;
import eu.egm.mapping.MappingDefinition;

import java.util.List;
import java.util.Map;

/**
 * Central mapping definitions for CGMES and IIDM DTO projections.
 *
 * Keeping these definitions together makes the transformer behavior explicit
 * and ready for externalization to XML/YAML configuration later.
 */
public class CgmesIidmMappingConfiguration extends MappingConfiguration {
    public static final String CGMES_EQUIPMENT_TO_IIDM_EQUIPMENT = "cgmes-equipment-to-iidm-equipment";
    public static final String IIDM_EQUIPMENT_TO_CGMES_EQUIPMENT = "iidm-equipment-to-cgmes-equipment";

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

    public CgmesIidmMappingConfiguration() {
        register(cgmesEquipmentToIidmEquipmentDefinition());
        register(iidmEquipmentToCgmesEquipmentDefinition());
    }

    public MappingDefinition cgmesEquipmentToIidmEquipment() {
        return definition(CGMES_EQUIPMENT_TO_IIDM_EQUIPMENT);
    }

    public MappingDefinition iidmEquipmentToCgmesEquipment() {
        return definition(IIDM_EQUIPMENT_TO_CGMES_EQUIPMENT);
    }

    private static MappingDefinition cgmesEquipmentToIidmEquipmentDefinition() {
        return new MappingDefinition(
                CGMES_EQUIPMENT_TO_IIDM_EQUIPMENT,
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

    private static MappingDefinition iidmEquipmentToCgmesEquipmentDefinition() {
        return new MappingDefinition(
                IIDM_EQUIPMENT_TO_CGMES_EQUIPMENT,
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
