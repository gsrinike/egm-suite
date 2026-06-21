package eu.egm.data.cgm.mapping;

import eu.egm.data.cgm.dto.cgmes.*;
import eu.egm.data.cgm.dto.iidm.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CgmProfileGraphLoader {
    private static final int MAX_ZIP_ENTRIES = 100;
    private static final int MAX_ZIP_ENTRY_BYTES = 100 * 1024 * 1024;

    public CgmProfileGraph load(byte[] payload) {
        if (CgmesPayloads.isZip(payload)) {
            return loadZip(payload);
        }
        return loadXml(payload, "uploaded XML");
    }

    private CgmProfileGraph loadZip(byte[] payload) {
        List<CgmProfileGraph.CgmGraphNode> nodes = new ArrayList<>();
        Map<String, String> metadata = new LinkedHashMap<>();
        int entries = 0;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(payload))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                entries++;
                if (entries > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException("CGM ZIP contains too many entries");
                }
                if (!isXmlEntry(entry.getName())) {
                    continue;
                }
                CgmProfileGraph graph = loadXml(readZipEntry(zipInputStream, entry.getName()), entry.getName());
                nodes.addAll(graph.nodes());
                metadata.putAll(graph.profileMetadata());
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to load CGM profile graph from ZIP payload", exception);
        }
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("CGM ZIP did not contain supported XML/RDF entries");
        }
        metadata.put("entryCount", Integer.toString(entries));
        return new CgmProfileGraph(nodes, metadata);
    }

    private CgmProfileGraph loadXml(byte[] payload, String sourceName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(payload)));
            List<CgmProfileGraph.CgmGraphNode> nodes = new ArrayList<>();
            walk(document.getDocumentElement(), nodes);
            return new CgmProfileGraph(nodes, Map.of("source", sourceName));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to load CGM profile graph from " + sourceName, exception);
        }
    }

    private void walk(Element element, List<CgmProfileGraph.CgmGraphNode> nodes) {
        String id = firstNonBlank(attribute(element, "ID"), attribute(element, "about"), attribute(element, "resource"));
        if (id != null) {
            nodes.add(new CgmProfileGraph.CgmGraphNode(stripReference(id), element.getLocalName(), values(element), references(element)));
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                walk(childElement, nodes);
            }
        }
    }

    private Map<String, String> values(Element element) {
        Map<String, String> values = attributes(element);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement
                    && attribute(childElement, "resource") == null
                    && childElement.getTextContent() != null
                    && !childElement.getTextContent().isBlank()) {
                values.put(key(childElement), childElement.getTextContent().trim());
            }
        }
        return values;
    }

    private Map<String, String> references(Element element) {
        Map<String, String> references = new LinkedHashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                String resource = attribute(childElement, "resource");
                if (resource != null) {
                    references.put(key(childElement), stripReference(resource));
                }
            }
        }
        return references;
    }

    private Map<String, String> attributes(Element element) {
        Map<String, String> values = new LinkedHashMap<>();
        NamedNodeMap nodeMap = element.getAttributes();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node attribute = nodeMap.item(i);
            values.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        return values;
    }

    private boolean isXmlEntry(String name) {
        String normalized = name.toLowerCase();
        return normalized.endsWith(".xml") || normalized.endsWith(".rdf");
    }

    private byte[] readZipEntry(ZipInputStream zipInputStream, String entryName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = zipInputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_ZIP_ENTRY_BYTES) {
                throw new IllegalArgumentException("CGM ZIP entry is too large: " + entryName);
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private String key(Element element) {
        return element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
    }

    private String attribute(Element element, String suffix) {
        NamedNodeMap nodeMap = element.getAttributes();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node attribute = nodeMap.item(i);
            if (attribute.getNodeName().endsWith(suffix)) {
                return attribute.getNodeValue();
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stripReference(String value) {
        if (value == null) {
            return null;
        }
        return value.startsWith("#") ? value.substring(1) : value;
    }
}
