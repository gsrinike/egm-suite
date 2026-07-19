package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import java.util.List;

interface ProfileExtractionStrategy {
    boolean supports(ProfileFamily family, String profileType);

    ProfilePayload<?> extract(ProfileFamily family, String profileType, String fileId, String objectId, List<RdfFact> facts);
}
