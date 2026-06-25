package eu.egm.data.cnm.ncp;

import eu.egm.data.cnm.common.RdfProfileReference;
import java.util.List;

/**
 * NCP-specific metadata extracted from RDF headers.
 */
public record NcpModelMetadata(
        String modelId,
        String processType,
        String created,
        List<RdfProfileReference> profiles) {
    public NcpModelMetadata {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }
}
