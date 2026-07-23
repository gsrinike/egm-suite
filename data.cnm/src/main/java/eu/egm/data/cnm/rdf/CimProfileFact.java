package eu.egm.data.cnm.rdf;

import java.util.Map;

/**
 * Compact CIM fact extracted from a streaming RDF parser.
 *
 * <p>The fact is intentionally flat: literals are kept in {@code attributes}
 * and resource links are kept in {@code references}. Profile stitching later
 * resolves the references through the mRID index.</p>
 */
public record CimProfileFact(
        String mRID,
        String cimType,
        String profileType,
        Map<String, Object> attributes,
        Map<String, String> references) {
    public CimProfileFact {
        mRID = valueOr(mRID);
        cimType = valueOr(cimType);
        profileType = valueOr(profileType);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        references = references == null ? Map.of() : Map.copyOf(references);
    }

    private static String valueOr(String value) {
        return value == null ? "" : value;
    }
}
