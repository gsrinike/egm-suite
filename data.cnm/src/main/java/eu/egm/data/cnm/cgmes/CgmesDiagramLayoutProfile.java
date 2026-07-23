package eu.egm.data.cnm.cgmes;

import java.util.List;

/**
 * CGMES Diagram Layout profile DTO.
 */
public record CgmesDiagramLayoutProfile(
        List<CgmesProfileEntity> diagrams,
        List<CgmesProfileEntity> diagramObjects,
        List<CgmesProfileEntity> diagramObjectPoints,
        List<CgmesProfileEntity> visibilityLayers) {
    public CgmesDiagramLayoutProfile {
        diagrams = diagrams == null ? List.of() : List.copyOf(diagrams);
        diagramObjects = diagramObjects == null ? List.of() : List.copyOf(diagramObjects);
        diagramObjectPoints = diagramObjectPoints == null ? List.of() : List.copyOf(diagramObjectPoints);
        visibilityLayers = visibilityLayers == null ? List.of() : List.copyOf(visibilityLayers);
    }
}
