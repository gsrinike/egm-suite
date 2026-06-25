package eu.egm.data.cnm.cgmes;

import eu.egm.data.cnm.common.RdfProfileReference;
import java.util.List;

/**
 * CGMES-specific metadata extracted from RDF headers.
 */
public record CgmesModelMetadata(
        String modelId,
        String scenarioTime,
        String created,
        List<RdfProfileReference> profiles) {
    public CgmesModelMetadata {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }
}
