# Repository Rules

## Active Module Shape

The active Maven reactor contains only these modules:

- `com.utils`
- `data.cnm`
- `com.mapping`
- `com.infra`
- `com.auth`
- `com.vault`
- `srv.cnm.services`
- `mock.srv.cnm.services`
- `gui.common`
- `gui.cnm.manager`

Keep `pom.xml` and `modules.yml` synchronized whenever modules are added or removed.

## Future Module Redevelopment

The CGM-related modules may be reintroduced with the same module names when the user asks for redesigned implementations. Treat that as new business logic, not as a restoration of the removed implementation.

When rebuilding those modules, use the existing `com.*` capabilities for configuration, mapping, infrastructure, authentication, and vault behavior instead of duplicating those concerns.

## Module Ownership

`com.*` modules own shared cross-cutting capabilities only:

- `com.utils`: environment resolution, YAML configuration loading, cache abstractions, REST service support, outbound HTTP client wiring, and bootstrap secret authorization contracts.
- `com.mapping`: generic mapping contracts and reflection-backed mapping behavior.
- `com.infra`: infrastructure adapters for document storage, object storage, messaging, and BPM process integration.
- `com.auth`: OIDC/OAuth2 authentication and runtime authorization services.
- `com.vault`: HashiCorp Vault and fallback secret resolution with authorization checks.

Keep application-specific workflow logic out of shared modules unless the capability is genuinely reusable.

CNM modules own Common Network Model application behavior:

- `data.cnm`: transport DTOs for CGMES, NCP, IIDM, import status, service type, and timeframe data.
- `srv.cnm.services`: Spring Boot REST service for RDF import, profile detection, raw object storage, and import metadata.
- `mock.srv.cnm.services`: mock REST service aligned with the CNM OpenAPI contract.
- `gui.common`: reusable Vue components and styling.
- `gui.cnm.manager`: Vue CNM manager application.

## Dependency Direction

- Shared modules should expose stable contracts and hide implementation details behind adapters or factories.
- `com.vault` depends on `com.utils` for bootstrap secret authorization.
- `com.auth` must not depend on `com.vault`.
- `com.infra` owns technology-specific dependencies such as Elasticsearch, MinIO, RabbitMQ, and Camunda.
- Service modules added in the future should depend on shared contracts and call external process runtimes over interfaces or HTTP, not by importing BPM process modules.
- `srv.cnm.services` depends on `data.cnm`, `com.utils`, and `com.infra`; it should not import GUI or mock modules.
- `gui.cnm.manager` depends on `gui.common` and calls CNM REST APIs through HTTP.
- `data.cnm` should not depend on Spring, PowSyBl, Elasticsearch, MinIO, or RabbitMQ.
- Do not add dependencies to `dependencies.xml` unless at least one active module directly needs them.

## Configuration Rules

All runtime configuration is YAML. Do not add XML application configuration.

Configuration loading follows the `com.utils` environment model:

- `env` JVM system property
- `ENV` environment variable
- default `local`

Module-specific config files follow this shape:

- `base/<module>-application.yml`
- `base/<module>-infra.yml`
- `base/<module>-vault.yml`
- `base/<module>-cache-config.yml`
- `<env>/<module>-application.yml`
- `<env>/<module>-infra.yml`
- `<env>/<module>-vault.yml`
- `<env>/<module>-cache-config.yml`

Environment-specific files override base files.

## Vault Rules

Vault placeholders use `${vault:SECRET_KEY}`.

When resolving placeholders:

- First load environment-specific and base module configuration through `com.utils`.
- If vault configuration is enabled and complete, resolve from HashiCorp Vault.
- If vault configuration is absent, fall back to environment variables and then loaded config values.
- Always authorize the client/key pair through `com.utils.secret.SecretAuthorizationService` before returning a secret.
- Avoid dependency cycles: `com.vault` depends on `com.utils`; `com.auth` remains independent from `com.vault`.

## Infrastructure Rules

`com.infra` owns infrastructure adapters under these packages:

- document storage: `com.infra.storage.document`
- Elasticsearch adapter: `com.infra.storage.document.elasticsearch`
- object storage: `com.infra.storage.object`
- MinIO adapter: `com.infra.storage.object.minio`
- events: `com.infra.event`
- RabbitMQ adapter: `com.infra.event.rabbitmq`
- BPM contracts: `com.infra.bpm`
- Camunda adapter: `com.infra.bpm.camunda`
- remote BPM adapter: `com.infra.bpm.remote`

Use `InfrastructureUtils` as the application-facing factory.

Object-storage buckets should be initialized during application startup or adapter initialization, not during every concurrent object upload. Concurrent upload paths must store objects without racing on bucket creation.

Remote BPM paths and tests should remain process-neutral in `com.infra`; concrete process routes belong in the process module that owns them.

## Documentation Rules

- Keep architecture documentation in `doc.arch`.
- Keep Codex codegen guidance in `doc.arch/codegen/codex`.
- Each active module should own a `README.md` explaining purpose, contents, implementation notes, and developer commands.
- Root `README.md` should describe the current active module map only.
- CNM design details belong in `doc.arch/CNM_IMPORT_DESIGN.md` and module READMEs.

## Verification Rules

Before finishing material code changes, run:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true test
```

For documentation-only changes, a targeted reference scan is enough.

Never commit Maven cache artifacts from `work/m2`.
