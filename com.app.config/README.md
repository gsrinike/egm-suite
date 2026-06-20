# com.app.config

`com.app.config` loads module configuration before Spring Boot creates the application context.

The loader resolves `env` through `com.env`, defaulting to `local`, and then loads configuration in this order:

1. `base/<module>-application.xml`
2. `base/<module>-infra.xml`
3. `base/<module>-cache-config.yml`
4. `<env>/<module>-application.xml`
5. `<env>/<module>-infra.xml`
6. `<env>/<module>-cache-config.yml`

Later files override earlier files through normal Spring property source ordering. XML files use Java properties XML syntax so keys remain compatible with Spring property names.

Applications must set `module` before `SpringApplication.run(...)`, or set the `MODULE` environment variable.

Cache behavior is loaded from `<module>-cache-config.yml` and represented as `CacheConfigurationService`. The cache provider is selected through the adapter factory in `com.utils`; use `config.cache.provider=java` for the in-memory cache or `none` to bypass caching.
