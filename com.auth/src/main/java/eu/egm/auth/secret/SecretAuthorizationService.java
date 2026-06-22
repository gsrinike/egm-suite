package eu.egm.auth.secret;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authorizes application access to secret keys before a secret provider returns a value.
 */
public class SecretAuthorizationService {
    public static final String ENABLED_PROPERTY = "vault.authorization.enabled";
    public static final String APPLICATION_ID_PROPERTY = "vault.authorization.application-id";
    public static final String ALLOWED_KEYS_PROPERTY = "vault.authorization.allowed-keys";

    private final boolean enabled;
    private final String applicationId;
    private final Set<String> allowedKeys;

    public SecretAuthorizationService(Map<String, Object> properties, String defaultApplicationId) {
        this.enabled = booleanValue(properties.get(ENABLED_PROPERTY), true);
        this.applicationId = stringValue(properties.get(APPLICATION_ID_PROPERTY), defaultApplicationId);
        this.allowedKeys = csv(properties.get(ALLOWED_KEYS_PROPERTY));
    }

    public SecretAccessDecision authorize(SecretAccessRequest request) {
        if (!enabled) {
            return SecretAccessDecision.grant();
        }
        if (request.applicationId() == null || request.applicationId().isBlank()) {
            return SecretAccessDecision.denied("Application id is required for secret access");
        }
        if (!request.applicationId().equals(applicationId)) {
            return SecretAccessDecision.denied("Application id is not authorized for this vault context");
        }
        if (allowedKeys.contains("*") || allowedKeys.contains(request.key())) {
            return SecretAccessDecision.grant();
        }
        return SecretAccessDecision.denied("Secret key is not authorized for this application");
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
