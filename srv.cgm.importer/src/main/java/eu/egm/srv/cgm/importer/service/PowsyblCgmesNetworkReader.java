package eu.egm.srv.cgm.importer.service;

import eu.egm.com.data.cgmes.EquipmentClassifier;
import eu.egm.com.data.cgmes.EquipmentType;
import eu.egm.com.data.cgmes.EquipmentView;
import eu.egm.com.data.cgmes.ImportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class PowsyblCgmesNetworkReader implements CgmesNetworkReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowsyblCgmesNetworkReader.class);
    private static final int ZIP_MAGIC_1 = 0x50;
    private static final int ZIP_MAGIC_2 = 0x4B;
    private static final int MAX_ZIP_ENTRIES = 100;
    private static final int MAX_ZIP_ENTRY_BYTES = 100 * 1024 * 1024;

    @Override
    public List<EquipmentView> read(String networkId, ImportMetadata metadata, InputStream inputStream) {
        try {
            byte[] payload = inputStream.readAllBytes();
            if (isZip(payload)) {
                return readZip(networkId, metadata, payload);
            }
            return readXml(networkId, metadata, payload, "uploaded XML");
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse CGMES payload", exception);
        }
    }

    private List<EquipmentView> readZip(String networkId, ImportMetadata metadata, byte[] payload) {
        List<EquipmentView> equipment = new ArrayList<>();
        int entries = 0;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(payload))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                entries++;
                if (entries > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException("CGMES ZIP contains too many entries");
                }
                if (!isXmlEntry(entry.getName())) {
                    LOGGER.debug("Skipping non XML CGMES ZIP entry {}", entry.getName());
                    continue;
                }
                byte[] entryBytes = readZipEntry(zipInputStream, entry.getName());
                equipment.addAll(readXml(networkId, metadata, entryBytes, entry.getName()));
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse CGMES ZIP payload", exception);
        }
        if (equipment.isEmpty()) {
            throw new IllegalArgumentException("CGMES ZIP did not contain supported XML/RDF entries");
        }
        LOGGER.info("Parsed {} equipment/state entries from {} CGMES ZIP entries for network {}", equipment.size(), entries, networkId);
        return equipment;
    }

    private List<EquipmentView> readXml(String networkId, ImportMetadata metadata, byte[] payload, String sourceName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(payload)));
            List<EquipmentView> equipment = new ArrayList<>();
            walk(document.getDocumentElement(), networkId, metadata, equipment);
            LOGGER.info("Parsed {} equipment/state CGMES elements from {} for network {}", equipment.size(), sourceName, networkId);
            return equipment;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse CGMES RDF/XML entry " + sourceName, exception);
        }
    }

    private boolean isZip(byte[] payload) {
        return payload.length >= 2
                && Byte.toUnsignedInt(payload[0]) == ZIP_MAGIC_1
                && Byte.toUnsignedInt(payload[1]) == ZIP_MAGIC_2;
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
                throw new IllegalArgumentException("CGMES ZIP entry is too large: " + entryName);
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private void walk(Element element, String networkId, ImportMetadata metadata, List<EquipmentView> equipment) {
        EquipmentType type = EquipmentClassifier.fromProfileClass(element.getLocalName());
        String id = firstNonBlank(attribute(element, "ID"), attribute(element, "about"), attribute(element, "resource"));
        if (type != EquipmentType.UNKNOWN && id != null) {
            Map<String, Object> attributes = attributes(element);
            String name = firstNonBlank(textByLocalName(element, "name"), textByLocalName(element, "IdentifiedObject.name"), id);
            String containerId = resourceBySuffix(element, "Equipment.EquipmentContainer");
            double nominalVoltage = parseDouble(firstNonBlank(textByLocalName(element, "nominalVoltage"), "0"));
            equipment.add(new EquipmentView(stripReference(id), networkId, metadata, name, type, stripReference(containerId), nominalVoltage, attributes));
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                walk(childElement, networkId, metadata, equipment);
            }
        }
    }

    private Map<String, Object> attributes(Element element) {
        Map<String, Object> values = new LinkedHashMap<>();
        NamedNodeMap nodeMap = element.getAttributes();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node attribute = nodeMap.item(i);
            values.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement && childElement.getTextContent() != null && !childElement.getTextContent().isBlank()) {
                values.put(childElement.getLocalName(), childElement.getTextContent().trim());
            }
        }
        return values.isEmpty() ? new HashMap<>() : values;
    }

    private String textByLocalName(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement
                    && (localName.equals(childElement.getLocalName()) || localName.equals(childElement.getNodeName()))
                    && childElement.getTextContent() != null) {
                return childElement.getTextContent().trim();
            }
        }
        return null;
    }

    private String resourceBySuffix(Element parent, String suffix) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement && childElement.getNodeName().endsWith(suffix)) {
                return attribute(childElement, "resource");
            }
        }
        return null;
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

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
