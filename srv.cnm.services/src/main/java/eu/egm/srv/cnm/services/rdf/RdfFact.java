package eu.egm.srv.cnm.services.rdf;

import java.util.Map;

record RdfFact(
        String mRID,
        String type,
        Map<String, Object> attributes,
        Map<String, String> references) {
    RdfFact {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        references = references == null ? Map.of() : Map.copyOf(references);
    }
}
