package eu.egm.auth.service;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimExtractor {

    public Set<String> roles(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        roles.addAll(extractRealmRoles(jwt));
        roles.addAll(extractClientRoles(jwt));
        return roles;
    }

    public Set<String> scopes(Jwt jwt) {
        Object scope = jwt.getClaims().get("scope");
        if (scope instanceof String scopeValue && !scopeValue.isBlank()) {
            return Set.of(scopeValue.split(" ")).stream()
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toSet());
        }
        Object scp = jwt.getClaims().get("scp");
        return extractStringSet(scp);
    }

    public Optional<String> tenantId(Jwt jwt) {
        return firstNonBlank(
                jwt.getClaimAsString("tenant_id"),
                jwt.getClaimAsString("tenant"),
                jwt.getClaimAsString("organization"),
                realmFromIssuer(jwt));
    }

    private Set<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return Set.of();
        }
        return extractStringSet(realmAccessMap.get("roles"));
    }

    private Set<String> extractClientRoles(Jwt jwt) {
        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (!(resourceAccess instanceof Map<?, ?> resourceAccessMap)) {
            return Set.of();
        }
        Set<String> roles = new HashSet<>();
        for (Object clientAccess : resourceAccessMap.values()) {
            if (clientAccess instanceof Map<?, ?> clientAccessMap) {
                roles.addAll(extractStringSet(clientAccessMap.get("roles")));
            }
        }
        return roles;
    }

    private Set<String> extractStringSet(Object value) {
        if (!(value instanceof Collection<?> values)) {
            return Set.of();
        }
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toSet());
    }

    private Optional<String> firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    private String realmFromIssuer(Jwt jwt) {
        if (jwt.getIssuer() == null) {
            return null;
        }
        String path = URI.create(jwt.getIssuer().toString()).getPath();
        String marker = "/realms/";
        int markerIndex = path.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        String realm = path.substring(markerIndex + marker.length());
        int slashIndex = realm.indexOf('/');
        return slashIndex < 0 ? realm : realm.substring(0, slashIndex);
    }
}
