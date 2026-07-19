package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import java.util.List;

/**
 * Immutable parse result for one RDF/XML model file.
 *
 * <p>The CNM import flow deliberately separates RDF/XML parsing from profile
 * mapping. This record is the boundary between those concerns: it contains the
 * model identity, profile declarations, detected profile family, detected
 * profile type, and flattened RDF facts that later strategy classes map into
 * typed DTO payloads.</p>
 */
record ParsedRdfModel(
        String modelId,
        ProfileFamily family,
        String profileType,
        List<RdfProfileReference> profiles,
        List<RdfFact> facts) {
    ParsedRdfModel {
        family = family == null ? ProfileFamily.Unknown : family;
        profileType = profileType == null ? "" : profileType;
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
        facts = facts == null ? List.of() : List.copyOf(facts);
    }
}
