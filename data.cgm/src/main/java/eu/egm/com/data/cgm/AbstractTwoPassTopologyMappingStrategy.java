package eu.egm.com.data.cgm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractTwoPassTopologyMappingStrategy implements CgmTopologyMappingStrategy {
    @Override
    public List<EquipmentView> project(String networkId, ImportMetadata metadata, CgmProfileGraph graph) {
        Map<String, EquipmentView> equipmentById = new LinkedHashMap<>();
        for (CgmProfileGraph.CgmGraphNode node : graph.nodes()) {
            EquipmentType type = EquipmentClassifier.fromProfileClass(node.type());
            if (type != EquipmentType.UNKNOWN) {
                equipmentById.put(node.id(), instantiate(networkId, metadata, node, type));
            }
        }
        for (CgmProfileGraph.CgmGraphNode node : graph.nodes()) {
            EquipmentView equipment = equipmentById.get(node.id());
            if (equipment != null) {
                equipmentById.put(node.id(), associate(equipment, node, equipmentById));
            }
        }
        return List.copyOf(equipmentById.values());
    }

    private EquipmentView instantiate(String networkId, ImportMetadata metadata, CgmProfileGraph.CgmGraphNode node, EquipmentType type) {
        return new EquipmentView(
                node.id(),
                networkId,
                metadata,
                firstNonBlank(node.value("IdentifiedObject.name"), node.value("name"), node.id()),
                type,
                null,
                parseDouble(firstNonBlank(node.value("ConductingEquipment.BaseVoltage"), node.value("nominalVoltage"), "0")),
                node.attributes());
    }

    private EquipmentView associate(EquipmentView equipment, CgmProfileGraph.CgmGraphNode node, Map<String, EquipmentView> equipmentById) {
        String containerId = firstNonBlank(
                node.reference("Equipment.EquipmentContainer"),
                node.reference("ConductingEquipment.BaseVoltage"),
                node.reference("Terminal.ConductingEquipment"),
                equipment.containerId());
        double nominalVoltage = equipment.nominalVoltage();
        String voltageLevelId = firstNonBlank(node.reference("Equipment.EquipmentContainer"), node.reference("ConnectivityNode.ConnectivityNodeContainer"));
        EquipmentView voltageLevel = equipmentById.get(voltageLevelId);
        if (nominalVoltage == 0 && voltageLevel != null) {
            nominalVoltage = voltageLevel.nominalVoltage();
        }
        return new EquipmentView(
                equipment.id(),
                equipment.networkId(),
                equipment.metadata(),
                equipment.name(),
                equipment.type(),
                containerId,
                nominalVoltage,
                equipment.attributes());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
