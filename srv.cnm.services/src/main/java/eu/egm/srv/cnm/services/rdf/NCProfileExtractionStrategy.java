package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.cnm.nc.NCProfileKind;
import eu.egm.data.cnm.nc.NCProfilePayload;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class NCProfileExtractionStrategy extends AbstractProfileExtractionStrategy {
    @Override
    public boolean supports(ProfileFamily family, String profileType) {
        return family == ProfileFamily.NCP || NCProfileKind.isKnown(profileType);
    }

    @Override
    public ProfilePayload<?> extract(
            ProfileProcessingContext context,
            List<RdfFact> facts) {
        NCProfileKind kind = NCProfileKind.fromCode(context.profileType());
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
                kind == NCProfileKind.UNKNOWN ? context.profileType() : kind.code(),
                context.fileId(),
                context.objectId(),
                topologyObjects(facts, context.profileType()),
                topologyRelations(facts),
                List.of(),
                new NCProfilePayload(kind, context.profileType(), entities));
    }
}
