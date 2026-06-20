package eu.egm.com.auth.service;

import eu.egm.com.auth.model.AuthorizationCheckRequest;
import eu.egm.com.auth.model.AuthorizationCheckResponse;
import eu.egm.com.auth.model.UserProfile;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationService.class);
    private final JwtDecoder jwtDecoder;
    private final JwtClaimExtractor claimExtractor;

    public AuthorizationService(JwtDecoder jwtDecoder, JwtClaimExtractor claimExtractor) {
        this.jwtDecoder = jwtDecoder;
        this.claimExtractor = claimExtractor;
    }

    public AuthorizationCheckResponse check(String authorizationHeader, AuthorizationCheckRequest request) {
        Optional<String> token = extractBearerToken(authorizationHeader);
        if (token.isEmpty()) {
            return denied("Missing bearer token");
        }
        try {
            Jwt jwt = jwtDecoder.decode(token.get());
            UserProfile profile = profile(jwt);
            Optional<String> requiredTenant = Optional.ofNullable(request.tenantId())
                    .map(String::trim)
                    .filter(value -> !value.isBlank());
            boolean tenantMatch = requiredTenant
                    .map(value -> value.equals(profile.tenantId()))
                    .orElse(true);
            Set<String> requiredRoles = emptyIfNull(request.requiredRoles());
            Set<String> requiredScopes = emptyIfNull(request.requiredScopes());
            boolean roleMatch = profile.roles().containsAll(requiredRoles);
            boolean scopeMatch = profile.scopes().containsAll(requiredScopes);
            boolean allowed = tenantMatch && roleMatch && scopeMatch;
            String reason = reason(allowed, tenantMatch);
            LOGGER.info("Authorization check for subject={} tenant={} method={} path={} allowed={}",
                    profile.subject(), profile.tenantId(), request.method(), request.path(), allowed);
            return new AuthorizationCheckResponse(allowed, profile.subject(), profile.username(),
                    profile.tenantId(), profile.roles(), profile.scopes(), profile.expiresAt(), reason);
        } catch (JwtException ex) {
            LOGGER.warn("Authorization check rejected invalid token: {}", ex.getMessage());
            return denied("Invalid bearer token");
        }
    }

    public UserProfile profile(Jwt jwt) {
        return new UserProfile(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                claimExtractor.tenantId(jwt).orElse(null),
                claimExtractor.roles(jwt),
                claimExtractor.scopes(jwt),
                jwt.getExpiresAt());
    }

    private AuthorizationCheckResponse denied(String reason) {
        return new AuthorizationCheckResponse(false, null, null, null, Set.of(), Set.of(), null, reason);
    }

    private Optional<String> extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(authorizationHeader.substring("Bearer ".length()).trim())
                .filter(value -> !value.isBlank());
    }

    private Set<String> emptyIfNull(Set<String> values) {
        return values == null ? Set.of() : values;
    }

    private String reason(boolean allowed, boolean tenantMatch) {
        if (allowed) {
            return "Allowed";
        }
        if (!tenantMatch) {
            return "Token tenant does not match requested tenant";
        }
        return "Required roles or scopes are missing";
    }
}
