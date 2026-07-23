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
            ProfileProcessingContext context,
            List<RdfFact> facts) {
        return new ProfilePayload<>(
                context.profileFamily(),
                context.profileType(),
                context.fileId(),
                context.objectId(),
                topologyObjects(facts, context.profileType()),
                topologyRelations(facts),
                List.of("Profile type is not handled by a specialized extractor"),
                entities(facts));
    }
}
