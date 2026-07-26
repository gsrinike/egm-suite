package eu.egm.srv.common.lfsa.config;

import eu.egm.data.common.lfsa.common.LoadFlowParametersDto;
import eu.egm.data.common.lfsa.common.LoadFlowStrategy;
import eu.egm.data.common.lfsa.common.SecurityAnalysisParametersDto;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads LFSA defaults lazily and keeps them cached for concurrent run workers.
 */
@Service
public class LfSaDefaultsService {
    private static final String DEFAULTS_LOCATION = "config/security-analysis/default.yaml";

    private final AtomicReference<LfSaDefaults> cached = new AtomicReference<>();

    public LfSaDefaults load() {
        LfSaDefaults current = cached.get();
        if (current != null) {
            return current;
        }
        LfSaDefaults loaded = loadUncached();
        return cached.compareAndExchange(null, loaded) == null ? loaded : cached.get();
    }

    private LfSaDefaults loadUncached() {
        ClassPathResource resource = new ClassPathResource(DEFAULTS_LOCATION);
        if (!resource.exists()) {
            return fallback();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            Object raw = new Yaml().load(inputStream);
            Map<String, Object> values = flatten(raw);
            return new LfSaDefaults(
                    intValue(values, "limits.maxSearchImports", 1000),
                    intValue(values, "limits.maxSearchRuns", 1000),
                    intValue(values, "limits.maxDiagnostics", 50),
                    intValue(values, "limits.maxLineFlows", 500),
                    intValue(values, "limits.maxIidmNetworks", 500),
                    enumValue(
                            LoadFlowStrategy.class,
                            stringValue(values, "loadFlow.strategy", "DC_ONLY"),
                            LoadFlowStrategy.DC_ONLY),
                    loadFlowParameters(values),
                    securityAnalysisParameters(values));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load LFSA defaults from " + DEFAULTS_LOCATION, exception);
        }
    }

    private LfSaDefaults fallback() {
        return new LfSaDefaults(
                1000,
                1000,
                50,
                500,
                500,
                LoadFlowStrategy.DC_ONLY,
                new LoadFlowParametersDto(
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        "PREVIOUS_VALUES",
                        "PROPORTIONAL_TO_GENERATION_P",
                        "MAIN_CONNECTED",
                        true,
                        1.0),
                new SecurityAnalysisParametersDto(true, true, true, false, "", "LINE", 25));
    }

    private LoadFlowParametersDto loadFlowParameters(Map<String, Object> values) {
        return new LoadFlowParametersDto(
                booleanValue(values, "loadFlow.parameters.distributedSlack", true),
                booleanValue(values, "loadFlow.parameters.useReactiveLimits", true),
                booleanValue(values, "loadFlow.parameters.transformerVoltageControlOn", true),
                booleanValue(values, "loadFlow.parameters.phaseShifterRegulationOn", true),
                booleanValue(values, "loadFlow.parameters.shuntCompensatorVoltageControlOn", true),
                booleanValue(values, "loadFlow.parameters.readSlackBus", false),
                booleanValue(values, "loadFlow.parameters.writeSlackBus", false),
                stringValue(values, "loadFlow.parameters.voltageInitMode", "PREVIOUS_VALUES"),
                stringValue(values, "loadFlow.parameters.balanceType", "PROPORTIONAL_TO_GENERATION_P"),
                stringValue(values, "loadFlow.parameters.componentMode", "MAIN_CONNECTED"),
                booleanValue(values, "loadFlow.parameters.hvdcAcEmulation", true),
                doubleValue(values, "loadFlow.parameters.dcPowerFactor", 1.0));
    }

    private SecurityAnalysisParametersDto securityAnalysisParameters(Map<String, Object> values) {
        return new SecurityAnalysisParametersDto(
                booleanValue(values, "securityAnalysis.parameters.voltageLimitsChecked", true),
                booleanValue(values, "securityAnalysis.parameters.currentLimitsChecked", true),
                booleanValue(values, "securityAnalysis.parameters.activePowerLimitsChecked", true),
                booleanValue(values, "securityAnalysis.parameters.intermediateResultsInOperatorStrategy", false),
                stringValue(values, "securityAnalysis.parameters.debugDir", ""),
                stringValue(values, "securityAnalysis.contingencies.elementType", "LINE"),
                intValue(values, "securityAnalysis.contingencies.maxGenerated", 25));
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
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
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

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
