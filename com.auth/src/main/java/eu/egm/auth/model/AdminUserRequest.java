package eu.egm.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record AdminUserRequest(
        @NotBlank String username,
        @Email String email,
        String firstName,
        String lastName,
        boolean enabled,
        Set<String> realmRoles
) {
}
