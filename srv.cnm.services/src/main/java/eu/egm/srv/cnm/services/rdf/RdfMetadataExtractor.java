package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.cnm.common.RdfProfileReference;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Extracts profile-aware RDF/XML metadata into typed CNM DTO payloads.
 */
@Component
public class RdfMetadataExtractor {
    private final List<ProfileExtractionStrategy> strategies = List.of(
            new CgmesProfileExtractionStrategy(),
            new NcpProfileExtractionStrategy(),
            new UnknownProfileExtractionStrategy());

    public RdfMetadata extract(byte[] payload) {
        return extract(payload, ProfileFamily.Unknown, "", "", "");
    }

    public RdfMetadata extract(
            byte[] payload,
            ProfileFamily filenameFamily,
            String filenameProfileType,
            String fileId,
            String objectId) {
        try {
            Document document = parse(payload);
            Element root = document.getDocumentElement();
            String modelId = attribute(root, "about");
            List<RdfProfileReference> profiles = conformsToProfiles(document);
            ProfileFamily rdfFamily = profiles.stream()
                    .map(RdfProfileReference::family)
                    .filter(value -> value != ProfileFamily.Unknown)
                    .findFirst()
                    .orElse(ProfileFamily.Unknown);
            ProfileFamily family = filenameFamily == null || filenameFamily == ProfileFamily.Unknown
                    ? rdfFamily
                    : filenameFamily;
            String profileType = valueOr(filenameProfileType, profileTypeFromProfiles(profiles));
            List<RdfFact> facts = facts(document, profileType);
            ProfileExtractionStrategy strategy = strategies.stream()
                    .filter(candidate -> candidate.supports(family, profileType))
                    .findFirst()
                    .orElseThrow();
            ProfilePayload<?> profilePayload = strategy.extract(family, profileType, fileId, objectId, facts);
            Map<String, Long> entityCounts = facts.stream()
                    .collect(Collectors.groupingBy(RdfFact::type, LinkedHashMap::new, Collectors.counting()));
            return new RdfMetadata(
                    modelId,
                    family,
                    profileType,
                    detectedProfileKind(family, profileType),
                    profileJsonType(family, profileType),
                    profiles,
                    entityCounts,
                    profilePayload.warnings(),
                    profilePayload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse RDF/XML metadata", exception);
        }
    }

    private Document parse(byte[] payload) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(payload));
    }

    private List<RdfFact> facts(Document document, String profileType) {
        NodeList nodes = document.getDocumentElement().getChildNodes();
        List<RdfFact> facts = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element element && !"FullModel".equals(element.getLocalName())) {
                String type = valueOr(element.getLocalName(), element.getNodeName());
                String mRID = valueOr(resourceId(element), profileType + "-" + index);
                Map<String, Object> attributes = new LinkedHashMap<>();
                Map<String, String> references = new LinkedHashMap<>();
                NodeList children = element.getChildNodes();
                for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                    Node child = children.item(childIndex);
                    if (child instanceof Element childElement) {
                        String key = valueOr(childElement.getLocalName(), childElement.getNodeName());
                        String resource = attribute(childElement, "resource");
                        if (resource != null && !resource.isBlank()) {
                            references.put(key, normalizeResourceId(resource));
                        } else {
                            String text = childElement.getTextContent();
                            if (text != null && !text.isBlank()) {
                                attributes.put(normalizedKey(key), text.trim());
                            }
                        }
                    }
                }
                attributes.putIfAbsent("mRID", mRID);
                attributes.putIfAbsent("type", type);
                facts.add(new RdfFact(mRID, type, attributes, references));
            }
        }
        return facts;
    }

    private String resourceId(Element element) {
        return valueOr(
                normalizeResourceId(attribute(element, "ID")),
                normalizeResourceId(attribute(element, "about")));
    }

    private List<RdfProfileReference> conformsToProfiles(Document document) {
        NodeList nodes = document.getElementsByTagNameNS("*", "conformsTo");
        List<RdfProfileReference> profiles = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            String uri = attribute(node, "resource");
            if (uri != null && !uri.isBlank()) {
                profiles.add(new RdfProfileReference(family(uri), uri, version(uri)));
            }
        }
        return profiles;
    }

    private String attribute(Node node, String localName) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return null;
        }
        for (int index = 0; index < attributes.getLength(); index++) {
            Node attribute = attributes.item(index);
            if (localName.equals(attribute.getLocalName()) || localName.equals(attribute.getNodeName())) {
                return attribute.getNodeValue();
            }
        }
        return null;
    }

    private String normalizeResourceId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        int hash = trimmed.lastIndexOf('#');
        int slash = trimmed.lastIndexOf('/');
        int index = Math.max(hash, slash);
        return index >= 0 && index < trimmed.length() - 1 ? trimmed.substring(index + 1) : trimmed;
    }

    private ProfileFamily family(String uri) {
        String normalized = uri.toLowerCase(Locale.ROOT);
        if (normalized.contains("networkcode") || normalized.contains("ncp")) {
            return ProfileFamily.NCP;
        }
        if (normalized.contains("cgmes")
                || normalized.contains("iec61970-600")
                || normalized.contains("61970-600")
                || normalized.contains("ap-con.cim4.eu")) {
            return ProfileFamily.CGMES;
        }
        if (normalized.contains("cim4.eu")) {
            return ProfileFamily.NCP;
        }
        return ProfileFamily.Unknown;
    }

    private String version(String uri) {
        int slash = uri.lastIndexOf('/');
        return slash >= 0 && slash < uri.length() - 1 ? uri.substring(slash + 1) : "";
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
        return name == null ? "" : name.toUpperCase(Locale.ROOT);
    }

    private String detectedProfileKind(ProfileFamily family, String profileType) {
        return (family == ProfileFamily.NCP ? "NCP_" : "CGMES_") + valueOr(profileType, "UNKNOWN");
    }

    private String profileJsonType(ProfileFamily family, String profileType) {
        return (family == ProfileFamily.NCP ? "ncp" : "cgmes") + "." + valueOr(profileType, "unknown").toLowerCase(Locale.ROOT);
    }

    private String normalizedKey(String key) {
        int dot = key == null ? -1 : key.lastIndexOf('.');
        return dot >= 0 && dot < key.length() - 1 ? key.substring(dot + 1) : key;
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
