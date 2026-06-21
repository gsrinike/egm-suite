package eu.egm.com.data.cgm;

import com.powsybl.cgmes.model.CgmesNames;

import java.util.Locale;

public final class EquipmentClassifier {
    private EquipmentClassifier() {
    }

    public static EquipmentType fromProfileClass(String className) {
        if (className == null || className.isBlank()) {
            return EquipmentType.UNKNOWN;
        }
        String normalized = className.toLowerCase(Locale.ROOT);
        if (normalized.contains(".")) {
            return EquipmentType.UNKNOWN;
        }
        if (matches(normalized, CgmesNames.SUBSTATION)) {
            return EquipmentType.SUBSTATION;
        }
        if (matches(normalized, CgmesNames.VOLTAGE_LEVEL)) {
            return EquipmentType.VOLTAGE_LEVEL;
        }
        if (matches(normalized, CgmesNames.BUSBAR_SECTION) || normalized.endsWith("bus")) {
            return EquipmentType.BUS;
        }
        if (matches(normalized, CgmesNames.AC_LINE_SEGMENT) || normalized.endsWith("line")) {
            return EquipmentType.LINE;
        }
        if (matches(normalized, CgmesNames.POWER_TRANSFORMER)) {
            return EquipmentType.TRANSFORMER;
        }
        if (matches(normalized, CgmesNames.SYNCHRONOUS_MACHINE) || normalized.contains("generatingunit")) {
            return EquipmentType.GENERATOR;
        }
        if (matches(normalized, CgmesNames.ENERGY_CONSUMER) || normalized.contains("load")) {
            return EquipmentType.LOAD;
        }
        if (matches(normalized, CgmesNames.SHUNT_COMPENSATOR)) {
            return EquipmentType.SHUNT;
        }
        if (matches(normalized, CgmesNames.SWITCH) || CgmesNames.SWITCH_TYPES.stream().anyMatch(type -> matches(normalized, type))) {
            return EquipmentType.SWITCH;
        }
        if (normalized.startsWith("sv")) {
            return EquipmentType.STATE_VARIABLE;
        }
        return EquipmentType.UNKNOWN;
    }

    private static boolean matches(String normalizedClassName, String powsyblCgmesName) {
        return normalizedClassName.contains(powsyblCgmesName.toLowerCase(Locale.ROOT));
    }
}
