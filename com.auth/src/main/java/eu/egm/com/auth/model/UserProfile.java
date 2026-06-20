package eu.egm.com.auth.model;

import java.time.Instant;
import java.util.Set;

public record UserProfile(
        String subject,
        String username,
        String email,
        String tenantId,
        Set<String> roles,
        Set<String> scopes,
        Instant expiresAt
) {
}
