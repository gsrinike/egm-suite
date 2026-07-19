package eu.egm.data.cnm.cgmes;

import java.util.List;

/**
 * State Variables profile DTO containing solved electrical state values.
 */
public record CgmesStateVariablesProfile(
        List<CgmesProfileEntity> voltages,
        List<CgmesProfileEntity> flows,
        List<CgmesProfileEntity> injections) {
    public CgmesStateVariablesProfile {
        voltages = voltages == null ? List.of() : List.copyOf(voltages);
        flows = flows == null ? List.of() : List.copyOf(flows);
        injections = injections == null ? List.of() : List.copyOf(injections);
    }
}
