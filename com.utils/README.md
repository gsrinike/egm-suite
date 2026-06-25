# com.utils

`com.utils` contains generic utility code shared by multiple modules.

## Package Layout

- `com.utils.cache`: generic cache contracts, `CacheConfigurationService`, and `CacheServiceFactory`.
- `com.utils.cache.jdk`: in-memory Java cache implementation.
- `com.utils.env`: suite runtime environment resolution.
- `com.utils.config`: Spring Boot environment post-processing and module-scoped configuration loading.
- `com.utils.secret`: bootstrap-time secret access authorization contracts used during configuration resolution.
- `com.utils.restservice`: shared REST service support and outbound `RestTemplate` configuration.

## REST Service Support

`RestServiceSupport` supplies reusable logger, environment, module-name, and
observation-registry fields for REST-facing service implementations.

`RestServiceConfiguration` is opt-in Spring configuration. Runnable applications
import it to register the shared timeout-configured `RestTemplate`:

```java
@Import(RestServiceConfiguration.class)
```

`srv.cnm.services` and `mock.srv.cnm.services` currently import this configuration.

## Configuration Loading

`com.utils.env.EnvironmentResolverService` resolves the runtime environment from:

1. JVM system property `env`
2. Operating system environment variable `ENV`
3. Default value `local`

`com.utils.config.ConfigEnvironmentPostProcessor` uses that value to load module resources in this property-source order:

1. `<env>/<module>-application.yml`
2. `<env>/<module>-infra.yml`
3. `<env>/<module>-cache-config.yml`
4. `<env>/<module>-vault.yml`
5. `base/<module>-application.yml`
6. `base/<module>-infra.yml`
7. `base/<module>-cache-config.yml`
8. `base/<module>-vault.yml`

Earlier property sources have higher precedence, so environment-specific files override base defaults.

Configuration loading reads `CacheConfigurationService` from each module's cache configuration and creates the cache through `CacheServiceFactory`. The current provider values are `java` for the in-memory Java cache and `none` to disable caching without changing consuming code.

## Vault Configuration Inputs

`com.utils` loads optional `<module>-vault.yml` files but does not resolve secrets itself. When an application includes `com.vault`, the Vault post-processor resolves authorized placeholders after these raw property sources are available.

Configuration values can reference secrets through `com.vault`:

```yaml
utility:
  object-storage:
    access-key: "${vault:MINIO_SECRET_KEY}"
```

If the loaded application or vault configuration enables and configures HashiCorp Vault, the value is read from Vault. If Vault is not configured, the resolver falls back to the environment variable named inside the placeholder, then to a value with the same key in the loaded configuration. `com.vault` requires the client/key pair to be authorized through `com.utils.secret.SecretAuthorizationService` before any value is returned.
