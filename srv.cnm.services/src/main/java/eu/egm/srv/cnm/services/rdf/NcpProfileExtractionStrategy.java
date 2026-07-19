package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.cnm.ncp.NcpProfilePayload;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class NcpProfileExtractionStrategy extends AbstractProfileExtractionStrategy {
    @Override
    public boolean supports(ProfileFamily family, String profileType) {
        return family == ProfileFamily.NCP || "NCP".equalsIgnoreCase(profileType);
    }

    @Override
    public ProfilePayload<?> extract(
            ProfileFamily family,
            String profileType,
            String fileId,
            String objectId,
            List<RdfFact> facts) {
        List<Map<String, Object>> entities = facts.stream()
                .map(fact -> {
                    Map<String, Object> row = new LinkedHashMap<>(fact.attributes());
                    row.put("mRID", fact.mRID());
                    row.put("type", fact.type());
                    row.put("references", fact.references().size());
                    return row;
                })
                .toList();
        return new ProfilePayload<>(
                ProfileFamily.NCP,
                profileType,
                fileId,
                objectId,
                topologyObjects(facts, profileType),
                topologyRelations(facts),
                List.of(),
                new NcpProfilePayload(profileType, entities));
    }
}
