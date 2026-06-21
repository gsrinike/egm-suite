package eu.egm.auth.keycloak;

import eu.egm.auth.config.KeycloakProperties;
import eu.egm.auth.model.AdminUserRequest;
import eu.egm.auth.model.AdminUserResponse;
import eu.egm.auth.model.RoleRequest;
import eu.egm.auth.model.RoleResponse;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KeycloakAdminClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAdminClient.class);
    private final KeycloakProperties properties;
    private final RestClient restClient;

    public KeycloakAdminClient(KeycloakProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    public List<AdminUserResponse> listUsers(String search) {
        return restClient.get()
                .uri(properties.adminApiBaseUrl() + "/users?search={search}", search == null ? "" : search)
                .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public void createUser(AdminUserRequest request) {
        Map<String, Object> payload = Map.of(
                "username", request.username(),
                "email", request.email() == null ? "" : request.email(),
                "firstName", request.firstName() == null ? "" : request.firstName(),
                "lastName", request.lastName() == null ? "" : request.lastName(),
                "enabled", request.enabled());
        restClient.post()
                .uri(properties.adminApiBaseUrl() + "/users")
                .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        LOGGER.info("Created Keycloak user username={}", request.username());
    }

    public List<RoleResponse> listRealmRoles() {
        return restClient.get()
                .uri(properties.adminApiBaseUrl() + "/roles")
                .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public void createRealmRole(RoleRequest request) {
        restClient.post()
                .uri(properties.adminApiBaseUrl() + "/roles")
                .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", request.name(), "description", request.description() == null ? "" : request.description()))
                .retrieve()
                .toBodilessEntity();
        LOGGER.info("Created Keycloak realm role name={}", request.name());
    }

    public void assignRealmRole(String userId, String roleName) {
        RoleResponse role = realmRole(roleName);
        restClient.post()
                .uri(properties.adminApiBaseUrl() + "/users/{userId}/role-mappings/realm", userId)
                .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
        LOGGER.info("Assigned Keycloak realm role name={} to userId={}", roleName, userId);
    }

    private RoleResponse realmRole(String roleName) {
        return restClient.get()
                .uri(properties.adminApiBaseUrl() + "/roles/{roleName}", roleName)
                .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                .retrieve()
                .body(RoleResponse.class);
    }

    private String adminAccessToken() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.adminClientId());
        form.add("client_secret", properties.adminClientSecret());
        Map<String, Object> response = restClient.post()
                .uri(properties.tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        Object accessToken = response == null ? null : response.get("access_token");
        if (!(accessToken instanceof String token) || token.isBlank()) {
            throw new IllegalStateException("Keycloak admin token response did not contain access_token");
        }
        return token;
    }
}
