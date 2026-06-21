package eu.egm.data.cgm.mapping;

import eu.egm.data.cgm.dto.cgmes.*;
import eu.egm.data.cgm.dto.iidm.*;

import java.util.List;
import java.util.Map;

public class CgmIidmNetworkMapper {
    public IidmNetwork mapNetwork(String networkId, List<EquipmentView> equipment) {
        List<IidmEquipment> mapped = equipment == null ? List.of() : equipment.stream()
                .map(this::mapEquipment)
                .toList();
        return new IidmNetwork(networkId, networkId, mapped);
    }

    private IidmEquipment mapEquipment(EquipmentView equipment) {
        return new IidmEquipment(
                equipment.id(),
                equipment.name(),
                mapType(equipment.type()),
                equipment.containerId(),
                equipment.nominalVoltage(),
                equipment.attributes() == null ? Map.of() : equipment.attributes());
    }

    private IidmEquipmentType mapType(EquipmentType type) {
        return switch (type) {
            case SUBSTATION -> IidmEquipmentType.SUBSTATION;
            case VOLTAGE_LEVEL -> IidmEquipmentType.VOLTAGE_LEVEL;
            case BUS -> IidmEquipmentType.BUS;
            case LINE -> IidmEquipmentType.LINE;
            case TRANSFORMER -> IidmEquipmentType.TWO_WINDINGS_TRANSFORMER;
            case GENERATOR -> IidmEquipmentType.GENERATOR;
            case LOAD -> IidmEquipmentType.LOAD;
            case SHUNT -> IidmEquipmentType.SHUNT_COMPENSATOR;
            case SWITCH -> IidmEquipmentType.SWITCH;
            case STATE_VARIABLE -> IidmEquipmentType.STATE_VARIABLE;
            case UNKNOWN -> IidmEquipmentType.UNKNOWN;
        };
    }
}
