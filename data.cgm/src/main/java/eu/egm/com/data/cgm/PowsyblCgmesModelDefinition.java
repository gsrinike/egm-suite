package eu.egm.com.data.cgm;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.cgmes.model.CgmesSubset;

import java.util.HashSet;
import java.util.Set;

/**
 * Describes the PowSyBl CGMES model contract used by the local DTO module.
 *
 * This class deliberately exposes only stable model boundary types and CIM
 * names needed by import/search code. Runtime model creation remains in service
 * adapters, keeping DTOs and services loosely coupled.
 */
public final class PowsyblCgmesModelDefinition {
    public static final Class<CgmesModel> MODEL_TYPE = CgmesModel.class;
    public static final Set<CgmesSubset> SUPPORTED_SUBSETS = Set.of(
            CgmesSubset.EQUIPMENT,
            CgmesSubset.TOPOLOGY,
            CgmesSubset.STATE_VARIABLES,
            CgmesSubset.STEADY_STATE_HYPOTHESIS,
            CgmesSubset.EQUIPMENT_BOUNDARY,
            CgmesSubset.TOPOLOGY_BOUNDARY
    );
    public static final Set<String> SEARCHABLE_CIM_TYPES = searchableCimTypes();

    private PowsyblCgmesModelDefinition() {
    }

    private static Set<String> searchableCimTypes() {
        Set<String> names = new HashSet<>(Set.of(
                CgmesNames.SUBSTATION,
                CgmesNames.VOLTAGE_LEVEL,
                CgmesNames.BUSBAR_SECTION,
                CgmesNames.AC_LINE_SEGMENT,
                CgmesNames.POWER_TRANSFORMER,
                CgmesNames.SYNCHRONOUS_MACHINE,
                CgmesNames.ENERGY_CONSUMER,
                CgmesNames.SHUNT_COMPENSATOR,
                CgmesNames.SWITCH
        ));
        names.addAll(CgmesNames.SWITCH_TYPES);
        return Set.copyOf(names);
    }
}
