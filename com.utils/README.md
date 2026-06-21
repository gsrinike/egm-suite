# com.utils

`com.utils` contains generic utility code shared by multiple modules.

## Package Layout

- `com.utils.cache`: generic cache contracts, `CacheConfigurationService`, and `CacheServiceFactory`.
- `com.utils.cache.jdk`: in-memory Java cache implementation.
- `com.utils.env`: suite runtime environment resolution.
- `com.utils.config`: Spring Boot environment post-processing and module-scoped configuration loading.

## Configuration Loading

`com.utils.env.EnvironmentResolverService` resolves the runtime environment from:

1. JVM system property `env`
2. Operating system environment variable `ENV`
3. Default value `local`

`com.utils.config.ConfigEnvironmentPostProcessor` uses that value to load module resources in this order:

1. `base/<module>-application.xml`
2. `base/<module>-infra.xml`
3. `base/<module>-cache-config.yml`
4. `<env>/<module>-application.xml`
5. `<env>/<module>-infra.xml`
6. `<env>/<module>-cache-config.yml`

Configuration loading reads `CacheConfigurationService` from each module's cache configuration and creates the cache through `CacheServiceFactory`. The current provider values are `java` for the in-memory Java cache and `none` to disable caching without changing consuming code.
