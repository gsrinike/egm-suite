package eu.egm.data.cnm.cgmes;

import java.util.List;

/**
 * CGMES Geographical Location profile DTO.
 */
public record CgmesGeographicalLocationProfile(
        List<CgmesProfileEntity> locations,
        List<CgmesProfileEntity> positionPoints,
        List<CgmesProfileEntity> coordinateSystems) {
    public CgmesGeographicalLocationProfile {
        locations = locations == null ? List.of() : List.copyOf(locations);
        positionPoints = positionPoints == null ? List.of() : List.copyOf(positionPoints);
        coordinateSystems = coordinateSystems == null ? List.of() : List.copyOf(coordinateSystems);
    }
}
