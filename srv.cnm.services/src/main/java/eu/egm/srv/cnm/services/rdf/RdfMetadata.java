package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.cnm.common.RdfProfileReference;
import eu.egm.data.cnm.rdf.ProfileFragment;
import java.util.List;
import java.util.Map;

public record RdfMetadata(
        String modelId,
        ProfileFamily family,
        String profileType,
        String detectedProfileKind,
        String profileJsonType,
        List<RdfProfileReference> profiles,
        Map<String, Long> entityCounts,
        List<String> warnings,
        ProfileFragment fragment,
        ProfilePayload<?> payload) {
    public RdfMetadata {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
        entityCounts = entityCounts == null ? Map.of() : Map.copyOf(entityCounts);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
