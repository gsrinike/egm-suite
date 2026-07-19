package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Coordinates RDF metadata extraction for CNM imports.
 *
 * <p>The extractor is intentionally orchestration-only. It delegates RDF/XML
 * parsing to {@link RdfXmlProfileParser}, selects a profile mapping strategy
 * based on the detected family and profile code, and assembles the metadata
 * document that is later persisted in Elasticsearch. This keeps XML parsing,
 * profile DTO creation, and persistence metadata construction as separate
 * concerns.</p>
 */
@Component
public class RdfMetadataExtractor {
    private final RdfXmlProfileParser parser = new RdfXmlProfileParser();
    private final List<ProfileExtractionStrategy> strategies = List.of(
            new CgmesProfileExtractionStrategy(),
            new NcpProfileExtractionStrategy(),
            new UnknownProfileExtractionStrategy());

    /**
     * Extracts metadata when no filename-derived profile hints are available.
     *
     * @param payload raw RDF/XML bytes
     * @return profile-aware metadata and typed profile payload
     */
    public RdfMetadata extract(byte[] payload) {
        return extract(payload, ProfileFamily.Unknown, "", "", "");
    }

    /**
     * Extracts profile metadata and DTO payloads from one RDF/XML file.
     *
     * <p>Filename metadata is accepted as a hint because CGMES/NCP packages often
     * encode the profile type in the file name. RDF {@code conformsTo} metadata is
     * still parsed and used as a fallback when filename data is missing.</p>
     *
     * @param payload raw RDF/XML bytes
     * @param filenameFamily family inferred from the file name, if known
     * @param filenameProfileType profile code inferred from the file name, if known
     * @param fileId import file identifier used for cross-document navigation
     * @param objectId object-storage identifier used to retrieve the raw payload
     * @return metadata document contents ready for persistence
     */
    public RdfMetadata extract(
            byte[] payload,
            ProfileFamily filenameFamily,
            String filenameProfileType,
            String fileId,
            String objectId) {
        ParsedRdfModel model = parser.parse(payload, filenameFamily, filenameProfileType);
        ProfileExtractionStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.supports(model.family(), model.profileType()))
                .findFirst()
                .orElseThrow();
        ProfilePayload<?> profilePayload = strategy.extract(
                model.family(),
                model.profileType(),
                fileId,
                objectId,
                model.facts());
        Map<String, Long> entityCounts = model.facts().stream()
                .collect(Collectors.groupingBy(RdfFact::type, LinkedHashMap::new, Collectors.counting()));
        return new RdfMetadata(
                model.modelId(),
                model.family(),
                model.profileType(),
                detectedProfileKind(model.family(), model.profileType()),
                profileJsonType(model.family(), model.profileType()),
                model.profiles(),
                entityCounts,
                profilePayload.warnings(),
                profilePayload);
    }

    private String detectedProfileKind(ProfileFamily family, String profileType) {
        return (family == ProfileFamily.NCP ? "NCP_" : "CGMES_") + valueOr(profileType, "UNKNOWN");
    }

    private String profileJsonType(ProfileFamily family, String profileType) {
        return (family == ProfileFamily.NCP ? "ncp" : "cgmes") + "." + valueOr(profileType, "unknown").toLowerCase(Locale.ROOT);
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
