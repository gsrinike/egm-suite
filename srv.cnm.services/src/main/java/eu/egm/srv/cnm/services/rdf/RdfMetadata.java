package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import java.util.List;

public record RdfMetadata(
        String modelId,
        ProfileFamily family,
        List<RdfProfileReference> profiles) {
    public RdfMetadata {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }
}
