package eu.egm.data.cnm.cgmes;

import java.util.List;

/**
 * Equipment profile DTO containing physical grid equipment and terminals.
 */
public record CgmesEquipmentProfile(
        List<CgmesProfileEntity> substations,
        List<CgmesProfileEntity> voltageLevels,
        List<CgmesProfileEntity> equipment,
        List<CgmesProfileEntity> terminals,
        List<CgmesProfileEntity> regulatingControls) {
    public CgmesEquipmentProfile {
        substations = substations == null ? List.of() : List.copyOf(substations);
        voltageLevels = voltageLevels == null ? List.of() : List.copyOf(voltageLevels);
        equipment = equipment == null ? List.of() : List.copyOf(equipment);
        terminals = terminals == null ? List.of() : List.copyOf(terminals);
        regulatingControls = regulatingControls == null ? List.of() : List.copyOf(regulatingControls);
    }
}
