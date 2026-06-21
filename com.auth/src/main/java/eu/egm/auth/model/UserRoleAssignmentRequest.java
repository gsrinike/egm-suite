package eu.egm.auth.model;

import jakarta.validation.constraints.NotBlank;

public record UserRoleAssignmentRequest(@NotBlank String roleName) {
}
