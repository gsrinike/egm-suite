package eu.egm.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import eu.egm.auth.model.AuthorizationCheckRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class AuthorizationServiceTest {

    private final JwtDecoder jwtDecoder = token -> jwt();
    private final AuthorizationService service = new AuthorizationService(jwtDecoder, new JwtClaimExtractor());

    @Test
    void allowsRequestWhenRequiredRoleAndScopeExist() {
        AuthorizationCheckRequest request = new AuthorizationCheckRequest("GET", "/api/cgm/imports",
                "egm", Set.of("grid_reader"), Set.of("openid"));

        var response = service.check("Bearer token", request);

        assertThat(response.allowed()).isTrue();
        assertThat(response.roles()).contains("grid_reader", "auth_admin");
        assertThat(response.scopes()).contains("openid", "profile");
        assertThat(response.tenantId()).isEqualTo("egm");
    }

    @Test
    void deniesRequestWhenRoleIsMissing() {
        AuthorizationCheckRequest request = new AuthorizationCheckRequest("POST", "/api/auth/admin/users",
                "egm", Set.of("missing_role"), Set.of("openid"));

        var response = service.check("Bearer token", request);

        assertThat(response.allowed()).isFalse();
        assertThat(response.reason()).isEqualTo("Required roles or scopes are missing");
    }

    @Test
    void deniesRequestWhenBearerTokenIsMissing() {
        AuthorizationCheckRequest request = new AuthorizationCheckRequest("GET", "/api/cgm/imports", "egm", Set.of(), Set.of());

        var response = service.check(null, request);

        assertThat(response.allowed()).isFalse();
        assertThat(response.reason()).isEqualTo("Missing bearer token");
    }

    @Test
    void deniesRequestWhenTenantDoesNotMatchToken() {
        AuthorizationCheckRequest request = new AuthorizationCheckRequest("GET", "/api/cgm/imports",
                "other-tenant", Set.of("grid_reader"), Set.of("openid"));

        var response = service.check("Bearer token", request);

        assertThat(response.allowed()).isFalse();
        assertThat(response.reason()).isEqualTo("Token tenant does not match requested tenant");
    }

    private Jwt jwt() {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "iss", "http://localhost:8081/realms/egm",
                        "sub", "user-1",
                        "preferred_username", "operator",
                        "email", "operator@example.test",
                        "scope", "openid profile",
                        "realm_access", Map.of("roles", List.of("grid_reader")),
                        "resource_access", Map.of("egm-auth", Map.of("roles", List.of("auth_admin")))));
    }
}
