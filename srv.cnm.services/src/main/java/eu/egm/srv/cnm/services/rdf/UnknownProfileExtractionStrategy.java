package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import java.util.List;

class UnknownProfileExtractionStrategy extends AbstractProfileExtractionStrategy {
    @Override
    public boolean supports(ProfileFamily family, String profileType) {
        return true;
    }

    @Override
    public ProfilePayload<?> extract(
            ProfileFamily family,
            String profileType,
            String fileId,
            String objectId,
            List<RdfFact> facts) {
        return new ProfilePayload<>(
                family == null ? ProfileFamily.Unknown : family,
                profileType,
                fileId,
                objectId,
                topologyObjects(facts, profileType),
                topologyRelations(facts),
                List.of("Profile type is not handled by a specialized extractor"),
                entities(facts));
    }
}
