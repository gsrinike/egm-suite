package eu.egm.srv.cnm.services.rdf;

import java.util.Map;

/**
 * Flattened RDF resource extracted from one top-level RDF/XML element.
 *
 * <p>Attributes contain literal child values such as names and measurements.
 * References contain resource links to other RDF objects and are resolved later
 * into topology relations by the profile extraction strategies.</p>
 */
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
