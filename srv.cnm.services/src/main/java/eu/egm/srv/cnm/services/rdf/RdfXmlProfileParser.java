package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import eu.egm.data.cnm.cgmes.CgmesProfileKind;
import eu.egm.data.cnm.nc.NCProfileKind;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;

/**
 * Streams RDF/XML into a neutral profile model used by the import pipeline.
 *
 * <p>The parser uses RDF4J Rio's streaming {@code RDFHandler} path. It does not
 * build a Jena/RDF4J in-memory graph model, which keeps CGMES parsing bounded
 * by compact DTO maps rather than semantic-web object retention.</p>
 */
class RdfXmlProfileParser {
    /**
     * Parses one RDF/XML payload and applies filename metadata as a trusted
     * fallback when the RDF profile header is incomplete.
     *
     * @param payload raw RDF/XML bytes
     * @param filenameFamily profile family inferred from the uploaded file name
     * @param filenameProfileType profile code inferred from the uploaded file name
     * @return neutral RDF parse result for downstream mapping strategies
     */
    ParsedRdfModel parse(byte[] payload, ProfileFamily filenameFamily, String filenameProfileType) {
        try {
            CgmStreamingRdfHandler handler = new CgmStreamingRdfHandler();
            RDFParser parser = Rio.createParser(RDFFormat.RDFXML);
            parser.setRDFHandler(handler);
            parser.parse(new ByteArrayInputStream(payload), "https://egm.local/cnm/rdf/");
            List<RdfProfileReference> profiles = handler.profiles();
            ProfileFamily rdfFamily = profiles.stream()
                    .map(RdfProfileReference::family)
                    .filter(value -> value != ProfileFamily.Unknown)
                    .findFirst()
                    .orElse(ProfileFamily.Unknown);
            ProfileFamily family = filenameFamily == null || filenameFamily == ProfileFamily.Unknown
                    ? rdfFamily
                    : filenameFamily;
            String profileType = valueOr(filenameProfileType, profileTypeFromProfiles(profiles));
            return new ParsedRdfModel(handler.modelId(), family, profileType, profiles, handler.facts(profileType));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to stream RDF/XML metadata", exception);
        }
    }

    private String profileTypeFromProfiles(List<RdfProfileReference> profiles) {
        return profiles.stream()
                .map(RdfProfileReference::uri)
                .filter(Objects::nonNull)
                .map(uri -> {
                    String[] parts = uri.split("/");
                    return parts.length > 1 ? parts[parts.length - 2] : "";
                })
                .map(this::profileCode)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private String profileCode(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (normalized.contains("equipmentboundary")) {
            return CgmesProfileKind.BOUNDARY_EQUIPMENT.code();
        }
        if (normalized.contains("topologyboundary")) {
            return CgmesProfileKind.BOUNDARY_TOPOLOGY.code();
        }
        if (normalized.contains("equipment")) {
            return "EQ";
        }
        if (normalized.contains("steadystate") || normalized.contains("steady-state")) {
            return "SSH";
        }
        if (normalized.contains("statevariable")) {
            return "SV";
        }
        if (normalized.contains("topology")) {
            return "TP";
        }
        if (normalized.contains("diagram")) {
            return "DL";
        }
        if (normalized.contains("geographical") || normalized.contains("geographic") || normalized.contains("location")) {
            return "GL";
        }
        for (NCProfileKind kind : NCProfileKind.values()) {
            if (kind != NCProfileKind.UNKNOWN && normalized.contains(kind.label().toLowerCase(Locale.ROOT).replace(" ", ""))) {
                return kind.code();
            }
        }
        CgmesProfileKind cgmesKind = CgmesProfileKind.fromCode(name);
        if (cgmesKind != CgmesProfileKind.UNKNOWN) {
            return cgmesKind.code();
        }
        NCProfileKind ncKind = NCProfileKind.fromCode(name);
        if (ncKind != NCProfileKind.UNKNOWN) {
            return ncKind.code();
        }
        return name == null ? "" : name.toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
