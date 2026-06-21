package eu.egm.auth.api;

import eu.egm.auth.keycloak.KeycloakAdminClient;
import eu.egm.auth.model.AdminUserRequest;
import eu.egm.auth.model.AdminUserResponse;
import eu.egm.auth.model.RoleRequest;
import eu.egm.auth.model.RoleResponse;
import eu.egm.auth.model.UserRoleAssignmentRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/admin")
@PreAuthorize("hasRole('auth_admin')")
public class AuthAdminController {

    private final KeycloakAdminClient keycloakAdminClient;

    public AuthAdminController(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(value = "search", required = false) String search) {
        return keycloakAdminClient.listUsers(search);
    }

    @PostMapping("/users")
    public ResponseEntity<Void> createUser(@Valid @RequestBody AdminUserRequest request) {
        keycloakAdminClient.createUser(request);
        return ResponseEntity.created(URI.create("/api/auth/admin/users/" + request.username())).build();
    }

    @GetMapping("/roles")
    public List<RoleResponse> roles() {
        return keycloakAdminClient.listRealmRoles();
    }

    @PostMapping("/roles")
    public ResponseEntity<Void> createRole(@Valid @RequestBody RoleRequest request) {
        keycloakAdminClient.createRealmRole(request);
        return ResponseEntity.created(URI.create("/api/auth/admin/roles/" + request.name())).build();
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Void> assignRole(
            @org.springframework.web.bind.annotation.PathVariable("userId") String userId,
            @Valid @RequestBody UserRoleAssignmentRequest request) {
        keycloakAdminClient.assignRealmRole(userId, request.roleName());
        return ResponseEntity.noContent().build();
    }
}
