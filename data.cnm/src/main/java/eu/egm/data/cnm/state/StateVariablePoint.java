package eu.egm.data.cnm.state;

import java.util.Map;

/**
 * Dynamic operating-state value extracted from SSH, SV, or incremental updates.
 */
public record StateVariablePoint(
        String mRID,
        String cimType,
        String profileType,
        Map<String, Object> values,
        Map<String, String> references) {
    public StateVariablePoint {
        values = values == null ? Map.of() : Map.copyOf(values);
        references = references == null ? Map.of() : Map.copyOf(references);
    }
}
