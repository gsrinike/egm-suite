# Design Principles

This document records the principles adopted in the Energy Grid Management Suite and how they are applied in the current codebase.

## 1. Modular Ownership

Each module has a narrow purpose and a clear owner boundary:

- `com.*` modules provide cross-cutting capabilities such as utility/cache/configuration loading, authorized secret lookup, infrastructure adapters, and authentication.

This keeps shared platform capabilities focused and prevents technology-specific details from leaking across module boundaries.

## 2. Dependency Direction

Dependencies flow from use-case modules toward stable contracts and utilities:

- Services may depend on required shared modules such as `com.utils`, `com.mapping`, and `com.infra`.
- `com.vault` depends on `com.utils` for bootstrap secret authorization. `com.auth` must not depend on `com.vault`.
- Shared mapping behavior belongs in `com.mapping`.
- Shared infrastructure behavior belongs in `com.infra` and is exposed through adapter interfaces.

The root Maven POM manages versions but does not force technology dependencies into every module.

## 3. Configuration Over Code

Runtime configuration is externalized into module-specific files:

- `base/<module>-application.yml`
- `base/<module>-infra.yml`
- `base/<module>-vault.yml`
- `base/<module>-cache-config.yml`
- `<env>/<module>-application.yml`
- `<env>/<module>-infra.yml`
- `<env>/<module>-vault.yml`
- `<env>/<module>-cache-config.yml`

Base configuration defines defaults. Environment folders override only what changes.

See [com.utils README](../com.utils/README.md).

## 4. Adapter and Factory Boundaries

Technology-specific behavior is hidden behind service interfaces and adapters:

- `com.infra.storage.document.DocumentRepositoryService` hides Elasticsearch access.
- `com.infra.storage.object.ObjectStorageService` hides MinIO access.
- `com.infra.event.EventPublisherService` hides RabbitMQ access.
- `com.infra.bpm.BusinessProcessService` hides Camunda process orchestration and monitoring access.
- `InfrastructureUtils` resolves concrete adapters.
- `VaultService` hides HashiCorp Vault and environment/config fallback secret lookup, while `com.utils.secret.SecretAuthorizationService` authorizes each client/key pair before a secret is returned.
- `MappingService` hides the mapping implementation used by domain transformers.

This makes backend services easier to test and keeps infrastructure replacement possible.

## 5. Local-First Developer Workflow

The project is designed to run locally with Maven and Docker Compose:

- Maven compiles/tests/builds modules.
- Docker Compose starts shared local dependencies such as Elasticsearch, MinIO, RabbitMQ, OpenTelemetry, and Keycloak.
- Docker image build/push behavior is controlled through Maven properties.

Developers can run targeted module builds with `-pl <module> -am` and disable Docker work with:

```bash
-Ddocker.skip.build=true -Ddocker.skip.push=true
```

## 6. Observability by Default in Services

Runnable backend modules include standard logging and OpenTelemetry dependencies when they emit runtime telemetry. Libraries and DTO modules avoid runtime observability dependencies.

## 7. Test Scope Matches Risk

Tests are kept close to the behavior being changed:

- Utility modules test configuration, cache, mapping, infrastructure, authorization, and vault behavior close to the owning package.
- Runnable service modules test controller validation, service orchestration, and adapter-facing behavior when such modules are added.

## 8. Documentation Beside the Module

Each module owns a `README.md` explaining purpose, contents, implementation notes, and developer commands. Architecture documents link to those module READMEs instead of duplicating every detail.
