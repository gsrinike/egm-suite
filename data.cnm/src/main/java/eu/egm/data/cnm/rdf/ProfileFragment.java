package eu.egm.data.cnm.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import java.util.List;
import java.util.Map;

/**
 * Parsed profile fragment for one RDF/XML file before cross-profile stitching.
 */
public record ProfileFragment(
        String importId,
        String fileId,
        String objectId,
        String modelId,
        ProfileFamily profileFamily,
        String profileType,
        String tsoName,
        String businessDay,
        String businessTime,
        String timeFrame,
        String version,
        List<RdfProfileReference> profileReferences,
        List<CimProfileFact> facts,
        Map<String, Long> entityCounts,
        List<String> diagnostics) {
    public ProfileFragment {
        profileFamily = profileFamily == null ? ProfileFamily.Unknown : profileFamily;
        profileReferences = profileReferences == null ? List.of() : List.copyOf(profileReferences);
        facts = facts == null ? List.of() : List.copyOf(facts);
        entityCounts = entityCounts == null ? Map.of() : Map.copyOf(entityCounts);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
