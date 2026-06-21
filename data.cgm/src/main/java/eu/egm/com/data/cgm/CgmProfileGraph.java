package eu.egm.com.data.cgm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CgmProfileGraph(
        List<CgmGraphNode> nodes,
        Map<String, String> profileMetadata
) {
    public CgmProfileGraph {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        profileMetadata = profileMetadata == null ? Map.of() : Map.copyOf(profileMetadata);
    }

    public boolean containsType(String localType) {
        return nodes.stream().anyMatch(node -> node.type().equals(localType));
    }

    public record CgmGraphNode(
            String id,
            String type,
            Map<String, String> values,
            Map<String, String> references
    ) {
        public CgmGraphNode {
            values = values == null ? Map.of() : Map.copyOf(values);
            references = references == null ? Map.of() : Map.copyOf(references);
        }

        public Map<String, Object> attributes() {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.putAll(values);
            references.forEach((key, value) -> attributes.put(key, value));
            return attributes;
        }

        public String value(String key) {
            return values.get(key);
        }

        public String reference(String key) {
            return references.get(key);
        }
    }
}
