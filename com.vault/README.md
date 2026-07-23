# com.vault

`com.vault` resolves authorized secret placeholders for modules that need
passwords or access keys during configuration loading.

## Responsibilities

- Resolve `${vault:KEY}` placeholders.
- Read from HashiCorp Vault when Vault is enabled and configured.
- Fall back to environment variables and loaded configuration when Vault is not
  configured.
- Authorize every client/key pair through `com.utils.secret` before returning a
  value.

## Main Types

- `VaultService`
- `HashicorpVaultService`
- `EnvironmentVaultService`
- `CompositeVaultService`
- `AuthorizedVaultService`
- `VaultServiceFactory`
- `VaultPlaceholderResolver`
- `VaultEnvironmentPostProcessor`

## Configuration

Modules can provide `base/<module>-vault.yml` and `<env>/<module>-vault.yml`.
Important keys include `vault.enabled`, `vault.address`, `vault.token`,
`vault.kv.*`, and `vault.authorization.*`.

Example:

```yaml
vault:
  authorization:
    client-id: srv.cnm.services
    allowed-keys: MINIO_SECRET_KEY
utility:
  object-storage:
    secret-key: "${vault:MINIO_SECRET_KEY}"
```

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl com.vault -am test
```
