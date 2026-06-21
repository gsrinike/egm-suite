package eu.egm.data.cgm.dto.cgmes;

import java.util.List;

public record NetworkDiff(
        String leftNetworkId,
        String rightNetworkId,
        List<EquipmentView> added,
        List<EquipmentView> removed,
        List<ChangedEquipment> changed
) {
    public record ChangedEquipment(EquipmentView left, EquipmentView right, List<String> changedFields) {
    }
}
