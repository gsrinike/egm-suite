# com.vault

`com.vault` provides a small HashiCorp Vault integration and a fallback secret resolver for modules that need passwords without depending on a specific runtime service.

`com.vault` depends on the bootstrap secret authorization contract in `com.utils`, not on `com.auth`. Every secret key lookup is checked before Vault, environment, or config fallback values are returned.

## Package Layout

- `com.vault.VaultService`: standalone secret lookup boundary.
- `com.vault.HashicorpVaultService`: HTTP client for HashiCorp Vault KV secrets.
- `com.vault.EnvironmentVaultService`: fallback lookup from environment variables and loaded configuration.
- `com.vault.CompositeVaultService`: tries configured Vault first, then fallback.
- `com.vault.AuthorizedVaultService`: calls `com.utils.secret.SecretAuthorizationService` before delegating secret lookup.
- `com.vault.VaultServiceFactory`: creates the right service from module vault configuration.
- `com.vault.VaultPlaceholderResolver`: resolves config values such as `${vault:MINIO_SECRET_KEY}`.
- `com.vault.VaultEnvironmentPostProcessor`: resolves authorized vault placeholders after `com.utils` loads module configuration.

## Configuration

Applications can add optional vault config files:

- `base/<module>-vault.yml`
- `<env>/<module>-vault.yml`

Supported keys:

- `vault.enabled`: `true` uses HashiCorp Vault before fallback; default is `false`.
- `vault.address`: Vault base URL, for example `http://localhost:8200`.
- `vault.token`: token value, or use environment variable `VAULT_TOKEN`.
- `vault.namespace`: optional Vault Enterprise namespace.
- `vault.kv.mount`: KV mount, default `secret`.
- `vault.kv.path`: secret path under the mount, default `application`.
- `vault.kv.version`: `1` or `2`, default `2`.
- `vault.authorization.client-id`: client/module id allowed to access configured keys.
- `vault.authorization.application-id`: compatibility alias for `vault.authorization.client-id`.
- `vault.authorization.allowed-keys`: comma-separated list of allowed secret keys; `*` allows all keys for that client.
- `vault.authorization.enabled`: defaults to `true`; set to `false` only for local/test contexts that intentionally bypass authorization.

If Vault is not enabled or not fully configured, `${vault:KEY}` resolves from environment variable `KEY`, then from already loaded configuration key `KEY`. The authorization check still runs before fallback values are returned.

Example:

```yaml
vault:
  authorization:
    client-id: sample.app
    allowed-keys: APP_SECRET

utility:
  credentials:
    password: "${vault:APP_SECRET}"
```

The module can also be used directly by creating a service through `VaultServiceFactory`.
