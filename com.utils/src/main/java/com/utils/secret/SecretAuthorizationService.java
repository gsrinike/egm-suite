package com.utils.secret;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authorizes client access to secret keys before a secret provider returns a value.
 */
public class SecretAuthorizationService {
    public static final String ENABLED_PROPERTY = "vault.authorization.enabled";
    public static final String CLIENT_ID_PROPERTY = "vault.authorization.client-id";
    public static final String APPLICATION_ID_PROPERTY = "vault.authorization.application-id";
    public static final String ALLOWED_KEYS_PROPERTY = "vault.authorization.allowed-keys";

    private final boolean enabled;
    private final String clientId;
    private final Set<String> allowedKeys;

    public SecretAuthorizationService(Map<String, Object> properties, String defaultClientId) {
        this.enabled = booleanValue(properties.get(ENABLED_PROPERTY), true);
        this.clientId = stringValue(first(properties.get(CLIENT_ID_PROPERTY), properties.get(APPLICATION_ID_PROPERTY)), defaultClientId);
        this.allowedKeys = csv(properties.get(ALLOWED_KEYS_PROPERTY));
    }

    public SecretAccessDecision authorize(SecretAccessRequest request) {
        if (!enabled) {
            return SecretAccessDecision.grant();
        }
        if (request.clientId() == null || request.clientId().isBlank()) {
            return SecretAccessDecision.denied("Client id is required for secret access");
        }
        if (!request.clientId().equals(clientId)) {
            return SecretAccessDecision.denied("Client id is not authorized for this vault context");
        }
        if (allowedKeys.contains("*") || allowedKeys.contains(request.key())) {
            return SecretAccessDecision.grant();
        }
        return SecretAccessDecision.denied("Secret key is not authorized for this client");
    }

    private Object first(Object value, Object fallback) {
        if (value instanceof String text && text.isBlank()) {
            return fallback;
        }
        return value == null ? fallback : value;
    }

    private boolean booleanValue(Object value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value.toString());
    }

    private String stringValue(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }

    private Set<String> csv(Object value) {
        if (value == null || value.toString().isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.toString().split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
