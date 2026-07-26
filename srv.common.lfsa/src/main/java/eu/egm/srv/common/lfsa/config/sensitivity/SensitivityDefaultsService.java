package eu.egm.srv.common.lfsa.config.sensitivity;

import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisParametersDto;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads sensitivity defaults lazily so run workers can share a stable in-memory configuration.
 */
@Service
public class SensitivityDefaultsService {
    private static final String DEFAULTS_LOCATION = "config/sensitivity/default.yaml";

    private final AtomicReference<SensitivityDefaults> cached = new AtomicReference<>();

    public SensitivityDefaults load() {
        SensitivityDefaults current = cached.get();
        if (current != null) {
            return current;
        }
        SensitivityDefaults loaded = loadUncached();
        return cached.compareAndExchange(null, loaded) == null ? loaded : cached.get();
    }

    private SensitivityDefaults loadUncached() {
        ClassPathResource resource = new ClassPathResource(DEFAULTS_LOCATION);
        if (!resource.exists()) {
            return fallback();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            Object raw = new Yaml().load(inputStream);
            Map<String, Object> values = flatten(raw);
            return new SensitivityDefaults(
                    intValue(values, "limits.maxSearchRuns", 1000),
                    intValue(values, "limits.maxDiagnostics", 50),
                    intValue(values, "limits.maxResultRows", 500),
                    new SensitivityAnalysisParametersDto(
                            booleanValue(values, "parameters.dc", true),
                            stringValue(values, "parameters.functionType", "BRANCH_ACTIVE_POWER_1"),
                            stringValue(values, "parameters.variableType", "INJECTION_ACTIVE_POWER"),
                            stringValue(values, "parameters.contingencyContext", "ALL"),
                            intValue(values, "parameters.maxMonitoredBranches", 25),
                            intValue(values, "parameters.maxVariables", 25),
                            intValue(values, "parameters.maxGeneratedContingencies", 25),
                            doubleValue(values, "parameters.flowFlowSensitivityValueThreshold", 0.0),
                            doubleValue(values, "parameters.voltageVoltageSensitivityValueThreshold", 0.0),
                            doubleValue(values, "parameters.flowVoltageSensitivityValueThreshold", 0.0),
                            doubleValue(values, "parameters.angleFlowSensitivityValueThreshold", 0.0),
                            stringValue(values, "parameters.operatorStrategiesCalculationMode", "NONE"),
                            stringValue(values, "parameters.debugDir", "")),
                    stringValue(values, "inputs.defaultPtdfObjectId", ""),
                    stringValue(values, "inputs.defaultLodfObjectId", ""),
                    stringValue(values, "inputs.defaultGlskObjectId", ""));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load sensitivity defaults from " + DEFAULTS_LOCATION, exception);
        }
    }

    private SensitivityDefaults fallback() {
        return new SensitivityDefaults(
                1000,
                50,
                500,
                new SensitivityAnalysisParametersDto(
                        true,
                        "BRANCH_ACTIVE_POWER_1",
                        "INJECTION_ACTIVE_POWER",
                        "ALL",
                        25,
                        25,
                        25,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        "NONE",
                        ""),
                "",
                "",
                "");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        flatten("", (Map<Object, Object>) map, values);
        return values;
    }

    private void flatten(String prefix, Map<Object, Object> source, Map<String, Object> target) {
        source.forEach((key, value) -> {
            String path = prefix.isBlank() ? String.valueOf(key) : prefix + "." + key;
            if (value instanceof Map<?, ?> nested) {
                Map<Object, Object> nestedMap = new LinkedHashMap<>();
                nested.forEach(nestedMap::put);
                flatten(path, nestedMap, target);
            } else {
                target.put(path, value);
            }
        });
    }

    private boolean booleanValue(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private int intValue(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double doubleValue(Map<String, Object> values, String key, double fallback) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String stringValue(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }
}
