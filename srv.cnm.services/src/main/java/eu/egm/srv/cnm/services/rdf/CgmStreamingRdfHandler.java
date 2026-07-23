package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * RDF4J streaming handler that flattens RDF/XML statements into compact facts.
 *
 * <p>The handler deliberately keeps only scalar attributes and resource
 * references keyed by subject. It avoids building a full RDF repository or DOM
 * tree, which keeps large CGMES profile files suitable for asynchronous import
 * workers.</p>
 */
final class CgmStreamingRdfHandler extends AbstractRDFHandler {
    private final Map<String, MutableFact> factsBySubject = new LinkedHashMap<>();
    private final List<RdfProfileReference> profiles = new ArrayList<>();
    private String modelId = "";

    @Override
    public void handleStatement(Statement statement) throws RDFHandlerException {
        String predicate = localName(statement.getPredicate());
        String subject = subjectId(statement.getSubject());
        Value object = statement.getObject();
        if ("type".equalsIgnoreCase(predicate) && object instanceof IRI iri) {
            mutable(subject).type = localName(iri);
            return;
        }
        if ("conformsTo".equalsIgnoreCase(predicate) && object instanceof IRI iri) {
            String uri = iri.stringValue();
            profiles.add(new RdfProfileReference(family(uri), uri, version(uri)));
            modelId = valueOr(modelId, subject);
            return;
        }
        if (object instanceof Literal literal) {
            mutable(subject).attributes.put(normalizedKey(predicate), literal.getLabel());
            return;
        }
        if (object instanceof Resource resource) {
            mutable(subject).references.put(normalizedKey(predicate), subjectId(resource));
        }
    }

    List<RdfProfileReference> profiles() {
        return List.copyOf(profiles);
    }

    String modelId() {
        return modelId;
    }

    List<RdfFact> facts(String profileType) {
        List<RdfFact> facts = new ArrayList<>();
        int index = 0;
        for (MutableFact mutable : factsBySubject.values()) {
            if (isFullModel(mutable)) {
                continue;
            }
            String type = valueOr(mutable.type, "Resource");
            String mRID = valueOr(String.valueOf(mutable.attributes.getOrDefault("mRID", "")), mutable.subject);
            mRID = valueOr(mRID, profileType + "-" + index++);
            mutable.attributes.putIfAbsent("mRID", mRID);
            mutable.attributes.putIfAbsent("type", type);
            facts.add(new RdfFact(mRID, type, mutable.attributes, mutable.references));
        }
        return facts;
    }

    private boolean isFullModel(MutableFact fact) {
        return "FullModel".equals(fact.type) || fact.references.containsKey("conformsTo");
    }

    private MutableFact mutable(String subject) {
        return factsBySubject.computeIfAbsent(subject, MutableFact::new);
    }

    private String subjectId(Resource resource) {
        if (resource instanceof BNode bNode) {
            return bNode.getID();
        }
        return normalizeResourceId(resource.stringValue());
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

    private String localName(IRI iri) {
        return localName(iri.stringValue());
    }

    private String localName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int hash = value.lastIndexOf('#');
        int slash = value.lastIndexOf('/');
        int dot = value.lastIndexOf('.');
        int index = Math.max(Math.max(hash, slash), dot);
        return index >= 0 && index < value.length() - 1 ? value.substring(index + 1) : value;
    }

    private String normalizedKey(String key) {
        int dot = key == null ? -1 : key.lastIndexOf('.');
        return dot >= 0 && dot < key.length() - 1 ? key.substring(dot + 1) : key;
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static final class MutableFact {
        private final String subject;
        private String type = "";
        private final LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> references = new LinkedHashMap<>();

        private MutableFact(String subject) {
            this.subject = subject;
        }
    }
}
