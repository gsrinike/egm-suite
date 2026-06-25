package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Extracts minimal profile metadata from RDF/XML payloads without binding the
 * service layer to a specific RDF engine.
 */
@Component
public class RdfMetadataExtractor {
    public RdfMetadata extract(byte[] payload) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(payload));
            Element root = document.getDocumentElement();
            String modelId = attribute(root, "about");
            List<RdfProfileReference> profiles = conformsToProfiles(document);
            ProfileFamily family = profiles.stream()
                    .map(RdfProfileReference::family)
                    .filter(value -> value != ProfileFamily.Unknown)
                    .findFirst()
                    .orElse(ProfileFamily.Unknown);
            return new RdfMetadata(modelId, family, profiles);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse RDF/XML metadata", exception);
        }
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

    private ProfileFamily family(String uri) {
        String normalized = uri.toLowerCase();
        if (normalized.contains("cgmes")
                || normalized.contains("iec61970-600")
                || normalized.contains("61970-600")
                || normalized.contains("ap-con.cim4.eu")) {
            return ProfileFamily.CGMES;
        }
        if (normalized.contains("cim4.eu") || normalized.contains("networkcode") || normalized.contains("ncp")) {
            return ProfileFamily.NCP;
        }
        return ProfileFamily.Unknown;
    }

    private String version(String uri) {
        int slash = uri.lastIndexOf('/');
        return slash >= 0 && slash < uri.length() - 1 ? uri.substring(slash + 1) : "";
    }
}
