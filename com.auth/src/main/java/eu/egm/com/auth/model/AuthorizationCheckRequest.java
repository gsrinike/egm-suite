package eu.egm.com.auth.model;

import java.util.Set;

/**
 * Gateway-friendly authorization request. Gloo, Istio, or another gateway can
 * map the incoming route decision into the requested roles/scopes carried here.
 */
public record AuthorizationCheckRequest(
        String method,
        String path,
        String tenantId,
        Set<String> requiredRoles,
        Set<String> requiredScopes
) {
}
