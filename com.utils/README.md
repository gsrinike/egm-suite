# com.utils

`com.utils` contains shared utility code used by runnable services and platform
modules.

## Packages

- `com.utils.env`: resolves the active environment from `env`, `ENV`, then
  `local`.
- `com.utils.config`: loads module-scoped YAML configuration into Spring
  property sources.
- `com.utils.cache`: cache contracts, cache configuration, and provider factory.
- `com.utils.cache.jdk`: in-memory Java cache provider.
- `com.utils.secret`: bootstrap secret access authorization contracts.
- `com.utils.restservice`: REST service base support and shared outbound
  `RestTemplate` configuration.
- `com.utils.profile`: cached profile/default configuration loading for
  conversion logic.

## Configuration Loading

Configuration is loaded from `src/main/resources/config` using the module name
and active environment:

```text
base/<module>-application.yml
base/<module>-infra.yml
base/<module>-cache-config.yml
base/<module>-vault.yml
<env>/<module>-application.yml
<env>/<module>-infra.yml
<env>/<module>-cache-config.yml
<env>/<module>-vault.yml
```

Environment files override base files.

## REST Support

`RestServiceSupport` provides logger, environment, module name, and observation
registry access for REST service implementations. Runnable services import
`RestServiceConfiguration` when they need the shared timeout-configured
`RestTemplate`.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl com.utils test
```
