# com.auth

`com.auth` is the Energy Grid Management Suite authentication and authorization service. It integrates with Keycloak through OIDC/OAuth 2.0 and exposes APIs that can be called directly by backend services or by an API gateway such as Gloo, Istio, or another Envoy-based gateway.

## Purpose

- Validate OIDC access tokens issued by Keycloak.
- Convert Keycloak realm/client roles into Spring Security authorities.
- Provide a gateway-friendly authorization endpoint for route decisions.
- Provide authenticated user profile information.
- Provide admin APIs for Keycloak user and realm-role management.
- Provide `SecretAuthorizationService` for modules such as `com.vault` to authorize secret key access.

## Runtime Endpoints

- `POST /api/authz/check`: validates a bearer token from the `Authorization` header and checks requested roles/scopes from the JSON body.
- `GET /api/auth/me`: returns the current authenticated user's subject, username, email, roles, scopes, and expiry.
- `GET /api/auth/admin/users`: searches Keycloak users. Requires `auth_admin` role.
- `POST /api/auth/admin/users`: creates a Keycloak user. Requires `auth_admin` role.
- `GET /api/auth/admin/roles`: lists Keycloak realm roles. Requires `auth_admin` role.
- `POST /api/auth/admin/roles`: creates a Keycloak realm role. Requires `auth_admin` role.
- `POST /api/auth/admin/users/{userId}/roles`: assigns a realm role to a Keycloak user. Requires `auth_admin` role.

## Configuration

The service sets `module=com.auth` at startup and uses `com.utils.config` to load:

- `base/com.auth-application.xml`
- `base/com.auth-infra.xml`
- `base/com.auth-cache-config.yml`
- `${env}/com.auth-application.xml`
- `${env}/com.auth-infra.xml`
- `${env}/com.auth-cache-config.yml`

When `env` or `ENV` is not set, `local` is used. The local configuration can still resolve environment variables such as:

```bash
AUTH_SERVER_PORT=8082
OIDC_ISSUER_URI=http://localhost:8081/realms/egm
OIDC_JWK_SET_URI=http://localhost:8081/realms/egm/protocol/openid-connect/certs
KEYCLOAK_BASE_URL=http://localhost:8081
KEYCLOAK_REALM=egm
KEYCLOAK_ADMIN_CLIENT_ID=egm-auth-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=change-me
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
```

The Keycloak admin client must use the `client_credentials` grant and have enough realm-management permissions to manage users and roles.

## Gateway Hook

Gateways can call `POST /api/authz/check` before routing to protected services. A typical request is:

```json
{
  "method": "GET",
  "path": "/api/cgm/imports",
  "tenantId": "egm",
  "requiredRoles": ["grid_reader"],
  "requiredScopes": ["openid"]
}
```

The bearer token to validate is supplied in the `Authorization` header. The response includes `allowed`, token subject, username, resolved roles/scopes, expiry, and a reason string.

For multitenant deployments, pass `tenantId` from the gateway route or tenant resolver. The service resolves the token tenant from `tenant_id`, `tenant`, `organization`, or the Keycloak issuer realm path such as `/realms/egm`; when a request tenant is supplied, it must match the token tenant.

## Implementation Notes

The service uses Spring Security's OAuth2 resource server support for local JWT validation. Keycloak-specific role extraction is implemented in `SecurityConfig` and `JwtClaimExtractor`, keeping the controller and gateway contract independent from Keycloak token structure.

Admin operations use Keycloak's REST API through `KeycloakAdminClient`. The adapter obtains its own admin access token with client credentials for each operation; production hardening can add token caching once operational metrics show it is needed.

`SecretAuthorizationService` is a lightweight policy boundary used by `com.vault`. It validates that an application id is allowed to load a requested secret key before the Vault module returns that secret from HashiCorp Vault, environment variables, or config fallback values.
