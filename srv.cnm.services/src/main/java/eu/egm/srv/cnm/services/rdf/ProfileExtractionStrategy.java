package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import java.util.List;

/**
 * Strategy contract for converting neutral RDF facts into family/profile DTOs.
 *
 * <p>CGMES, NCP, and unknown profile files share the same parser output but
 * produce different payload shapes. Implementations own those profile-specific
 * DTO decisions while the extractor owns strategy selection.</p>
 */
interface ProfileExtractionStrategy {
    /**
     * Determines whether this strategy can map the detected profile.
     */
    boolean supports(ProfileFamily family, String profileType);

    /**
     * Converts parsed RDF facts into a serializable profile payload.
     */
    ProfilePayload<?> extract(ProfileProcessingContext context, List<RdfFact> facts);
}
