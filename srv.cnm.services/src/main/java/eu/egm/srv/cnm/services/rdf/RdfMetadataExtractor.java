package eu.egm.srv.cnm.services.rdf;

import com.utils.profile.ProfileDefaults;
import com.utils.profile.ProfileDefaultsService;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.cnm.cgmes.CgmesProfileKind;
import eu.egm.data.cnm.nc.NCProfileKind;
import eu.egm.data.cnm.rdf.CimProfileFact;
import eu.egm.data.cnm.rdf.ProfileFragment;
import java.util.ArrayList;
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
            new NCProfileExtractionStrategy(),
            new UnknownProfileExtractionStrategy());
    private final ProfileDefaultsService profileDefaultsService;

    public RdfMetadataExtractor() {
        this(new ProfileDefaultsService());
    }

    RdfMetadataExtractor(ProfileDefaultsService profileDefaultsService) {
        this.profileDefaultsService = profileDefaultsService;
    }

    /**
     * Extracts metadata when no filename-derived profile hints are available.
     *
     * @param payload raw RDF/XML bytes
     * @return profile-aware metadata and typed profile payload
     */
    public RdfMetadata extract(byte[] payload) {
        return extract(payload, ProfileProcessingContext.forFile(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ProfileFamily.Unknown,
                ""));
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
        return extract(payload, ProfileProcessingContext.forFile(
                "",
                fileId,
                objectId,
                "",
                "",
                "",
                "",
                filenameFamily,
                filenameProfileType));
    }

    /**
     * Extracts profile metadata using a caller-provided processing context.
     *
     * @param payload raw RDF/XML bytes
     * @param context file processing context created by the import processor
     * @return metadata document contents ready for persistence
     */
    public RdfMetadata extract(byte[] payload, ProfileProcessingContext context) {
        ParsedRdfModel model = parser.parse(payload, context.profileFamily(), context.profileType());
        ProfileDefaults defaults = profileDefaults(model.family());
        ProfileProcessingContext detectedContext = context.withDetectedProfile(
                model.family(),
                model.profileType(),
                defaults);
        ProfileExtractionStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.supports(detectedContext.profileFamily(), detectedContext.profileType()))
                .findFirst()
                .orElseThrow();
        ProfilePayload<?> profilePayload = strategy.extract(detectedContext, model.facts());
        Map<String, Long> entityCounts = model.facts().stream()
                .collect(Collectors.groupingBy(RdfFact::type, LinkedHashMap::new, Collectors.counting()));
        List<String> warnings = new ArrayList<>(profilePayload.warnings());
        warnings.addAll(profileDefaultWarnings(detectedContext));
        return new RdfMetadata(
                model.modelId(),
                detectedContext.profileFamily(),
                detectedContext.profileType(),
                detectedProfileKind(detectedContext.profileFamily(), detectedContext.profileType()),
                profileJsonType(detectedContext.profileFamily(), detectedContext.profileType()),
                model.profiles(),
                entityCounts,
                warnings,
                fragment(detectedContext, model, entityCounts, warnings),
                profilePayload);
    }

    private ProfileFragment fragment(
            ProfileProcessingContext context,
            ParsedRdfModel model,
            Map<String, Long> entityCounts,
            List<String> warnings) {
        return new ProfileFragment(
                context.importId(),
                context.fileId(),
                context.objectId(),
                model.modelId(),
                context.profileFamily(),
                context.profileType(),
                context.tsoName(),
                context.businessDay(),
                context.businessTime(),
                context.timeFrame(),
                "",
                model.profiles(),
                model.facts().stream()
                        .map(fact -> new CimProfileFact(
                                fact.mRID(),
                                fact.type(),
                                context.profileType(),
                                fact.attributes(),
                                fact.references()))
                        .toList(),
                entityCounts,
                warnings);
    }

    private ProfileDefaults profileDefaults(ProfileFamily family) {
        if (family == ProfileFamily.Unknown) {
            return new ProfileDefaults("", Map.of());
        }
        String defaultsFamily = family == ProfileFamily.NCP ? "nc" : "cgmes";
        return profileDefaultsService.load(defaultsFamily, "defaults");
    }

    private List<String> profileDefaultWarnings(ProfileProcessingContext context) {
        if (context.profileType().isBlank() || context.profileFamily() == ProfileFamily.Unknown) {
            return List.of();
        }
        ProfileDefaults defaults = context.profileDefaults();
        if (defaults == null) {
            return List.of();
        }
        List<String> supportedKinds = defaults.stringList("profile.supported-kinds");
        if (!supportedKinds.isEmpty() && supportedKinds.stream().noneMatch(kind -> kind.equalsIgnoreCase(context.profileType()))) {
            return List.of("Profile type %s is not listed in %s".formatted(context.profileType(), defaults.source()));
        }
        return List.of();
    }

    private String detectedProfileKind(ProfileFamily family, String profileType) {
        if (family == ProfileFamily.NCP) {
            NCProfileKind kind = NCProfileKind.fromCode(profileType);
            return "NCP_" + (kind == NCProfileKind.UNKNOWN ? valueOr(profileType, "UNKNOWN") : kind.name());
        }
        CgmesProfileKind kind = CgmesProfileKind.fromCode(profileType);
        return "CGMES_" + (kind == CgmesProfileKind.UNKNOWN ? valueOr(profileType, "UNKNOWN") : kind.name());
    }

    private String profileJsonType(ProfileFamily family, String profileType) {
        return (family == ProfileFamily.NCP ? "ncp" : "cgmes") + "." + valueOr(profileType, "unknown").toLowerCase(Locale.ROOT);
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
