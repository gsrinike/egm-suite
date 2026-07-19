package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.cgmes.CgmesProfileEntity;
import eu.egm.data.cnm.common.GridTopologyObject;
import eu.egm.data.cnm.common.GridTopologyRelation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class AbstractProfileExtractionStrategy implements ProfileExtractionStrategy {
    protected List<GridTopologyObject> topologyObjects(List<RdfFact> facts, String profileType) {
        return facts.stream()
                .map(fact -> new GridTopologyObject(
                        fact.mRID(),
                        name(fact),
                        fact.type(),
                        profileType,
                        fact.attributes()))
                .toList();
    }

    protected List<GridTopologyRelation> topologyRelations(List<RdfFact> facts) {
        List<GridTopologyRelation> relations = new ArrayList<>();
        for (RdfFact fact : facts) {
            fact.references().forEach((type, target) -> relations.add(new GridTopologyRelation(
                    fact.mRID() + ":" + type + ":" + target,
                    fact.mRID(),
                    target,
                    type,
                    Map.of())));
        }
        return relations;
    }

    protected List<CgmesProfileEntity> entities(List<RdfFact> facts, String... typeTokens) {
        return facts.stream()
                .filter(fact -> matches(fact.type(), typeTokens))
                .map(fact -> new CgmesProfileEntity(fact.mRID(), name(fact), fact.type(), fact.attributes()))
                .toList();
    }

    protected boolean matches(String type, String... tokens) {
        if (tokens.length == 0) {
            return true;
        }
        String normalized = type == null ? "" : type.toLowerCase();
        for (String token : tokens) {
            if (normalized.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    protected String name(RdfFact fact) {
        Object name = fact.attributes().get("name");
        return name == null ? "" : String.valueOf(name);
    }
}
