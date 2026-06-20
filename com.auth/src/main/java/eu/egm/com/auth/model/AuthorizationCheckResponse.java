package eu.egm.com.auth.model;

import java.time.Instant;
import java.util.Set;

public record AuthorizationCheckResponse(
        boolean allowed,
        String subject,
        String username,
        String tenantId,
        Set<String> roles,
        Set<String> scopes,
        Instant expiresAt,
        String reason
) {
}
