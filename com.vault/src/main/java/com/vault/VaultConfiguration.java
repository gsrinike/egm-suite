package com.vault;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record VaultConfiguration(
        boolean enabled,
        URI address,
        String token,
        String namespace,
        String kvMount,
        String kvPath,
        int kvVersion,
        Duration timeout) {

    public static VaultConfiguration from(Map<String, Object> properties, Map<String, String> environment) {
        return new VaultConfiguration(
                booleanValue(properties.get("vault.enabled"), false),
                uriValue(first(properties.get("vault.address"), environment.get("VAULT_ADDR"))),
                stringValue(first(properties.get("vault.token"), environment.get("VAULT_TOKEN"))),
                stringValue(first(properties.get("vault.namespace"), environment.get("VAULT_NAMESPACE"))),
                stringValue(first(properties.get("vault.kv.mount"), "secret")),
                stringValue(first(properties.get("vault.kv.path"), "application")),
                intValue(properties.get("vault.kv.version"), 2),
                Duration.ofSeconds(intValue(properties.get("vault.timeout-seconds"), 5)));
    }

    public boolean canConnect() {
        return enabled && address != null && token != null && !token.isBlank();
    }

    private static Object first(Object value, Object fallback) {
        if (value instanceof String text && text.isBlank()) {
            return fallback;
        }
        return value == null ? fallback : value;
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static URI uriValue(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? null : URI.create(text);
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value.toString());
    }

    private static int intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Integer.parseInt(value.toString());
    }
}
