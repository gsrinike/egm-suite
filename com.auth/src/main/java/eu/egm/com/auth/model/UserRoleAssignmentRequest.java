package eu.egm.com.auth.model;

import jakarta.validation.constraints.NotBlank;

public record UserRoleAssignmentRequest(@NotBlank String roleName) {
}
