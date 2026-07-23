# com.auth

`com.auth` is the Keycloak-backed authentication and authorization service.

## Responsibilities

- Validate OIDC access tokens.
- Convert Keycloak realm/client roles into Spring Security authorities.
- Expose gateway-friendly authorization checks.
- Expose authenticated user profile information.
- Provide admin endpoints for Keycloak users and realm roles.

## Main Endpoints

- `POST /api/authz/check`
- `GET /api/auth/me`
- `GET /api/auth/admin/users`
- `POST /api/auth/admin/users`
- `GET /api/auth/admin/roles`
- `POST /api/auth/admin/roles`
- `POST /api/auth/admin/users/{userId}/roles`

Admin endpoints require the configured admin role.

## Configuration

The module name is `com.auth`. YAML configuration is loaded by `com.utils`.
Keycloak issuer, JWK set URI, admin client, realm, and OpenTelemetry settings
are supplied through module configuration or environment variables.

`com.auth` does not depend on `com.vault`.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl com.auth -am test
```
