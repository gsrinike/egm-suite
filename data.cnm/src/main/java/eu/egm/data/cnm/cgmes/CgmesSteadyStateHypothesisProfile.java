package eu.egm.data.cnm.cgmes;

import java.util.List;

/**
 * Steady State Hypothesis profile DTO containing operating values and setpoints.
 */
public record CgmesSteadyStateHypothesisProfile(
        List<CgmesProfileEntity> equipmentStates,
        List<CgmesProfileEntity> controlSetpoints,
        List<CgmesProfileEntity> terminalValues) {
    public CgmesSteadyStateHypothesisProfile {
        equipmentStates = equipmentStates == null ? List.of() : List.copyOf(equipmentStates);
        controlSetpoints = controlSetpoints == null ? List.of() : List.copyOf(controlSetpoints);
        terminalValues = terminalValues == null ? List.of() : List.copyOf(terminalValues);
    }
}
