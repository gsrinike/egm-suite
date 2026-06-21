package eu.egm.auth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.keycloak")
public record KeycloakProperties(
        @NotBlank String baseUrl,
        @NotBlank String realm,
        @NotBlank String adminClientId,
        @NotBlank String adminClientSecret
) {

    public String tokenEndpoint() {
        return "%s/realms/%s/protocol/openid-connect/token".formatted(baseUrl, realm);
    }

    public String adminApiBaseUrl() {
        return "%s/admin/realms/%s".formatted(baseUrl, realm);
    }
}
