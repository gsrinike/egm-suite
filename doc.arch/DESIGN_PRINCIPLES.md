# Design Principles

This document records the principles adopted in the Energy Grid Management Suite and how they are applied in the current codebase.

## 1. Modular Ownership

Each module has a narrow purpose and a clear owner boundary:

- `com.*` modules provide cross-cutting capabilities such as utility/cache/configuration loading, infrastructure adapters, and authentication.
- `data.*` modules define shared data contracts and PowSyBl-aligned domain vocabulary.
- `map.*` modules transform between data models.
- `srv.*` modules expose backend APIs and orchestrate use cases.
- `gui.*` modules provide user-facing React applications.

This keeps services focused on workflow orchestration instead of owning all technical details.

## 2. Dependency Direction

Dependencies flow from use-case modules toward stable contracts and utilities:

- Services may depend on required `data`, `com.utils`, and `com.infra` modules. CGM service modules keep mapping/runtime model behavior in `data.cgm`.
- Mapping modules may depend on source/target data modules and `com.mapping`.
- Data modules must not depend on infrastructure, Spring MVC, RabbitMQ, MinIO, or Elasticsearch.
- GUI modules communicate through HTTP contracts instead of importing backend Java code.

The root Maven POM manages versions but does not force technology dependencies into every module.

## 3. Configuration Over Code

Runtime configuration is externalized into module-specific files:

- `base/<module>-application.xml`
- `base/<module>-infra.xml`
- `base/<module>-cache-config.yml`
- `<env>/<module>-application.xml`
- `<env>/<module>-infra.xml`
- `<env>/<module>-cache-config.yml`

Base configuration defines defaults. Environment folders override only what changes.

See [com.utils README](../com.utils/README.md).

## 4. Adapter and Factory Boundaries

Technology-specific behavior is hidden behind service interfaces and adapters:

- `DocumentRepositoryService` hides Elasticsearch access.
- `ObjectStorageService` hides MinIO access.
- `EventPublisherService` hides RabbitMQ access.
- `InfrastructureAdapterFactory` resolves concrete adapters.
- `MappingService` hides the mapping implementation used by domain transformers.

This makes backend services easier to test and keeps infrastructure replacement possible.

## 5. PowSyBl Alignment Without Runtime Lock-In

The CGM data module aligns vocabulary with PowSyBl while keeping services free of PowSyBl dependencies:

- `data.cgm` owns CGMES DTOs in `eu.egm.data.cgm.dto.cgmes`.
- `data.cgm` owns IIDM DTOs in `eu.egm.data.cgm.dto.iidm`.
- `data.cgm` owns CGMES reading, PowSyBl conversion, fallback graph loading, topology strategies, and IIDM DTO projection under `eu.egm.data.cgm.mapping`.
- `map.cgm` performs reusable CGMES/IIDM transformations through `eu.egm.mapping.MappingService`.

Service modules consume DTO projections. Full PowSyBl runtime model creation remains isolated behind adapters so APIs and GUI contracts remain stable.

## 6. Local-First Developer Workflow

The project is designed to run locally with Maven and Docker Compose:

- Maven compiles/tests/builds modules.
- Docker Compose starts Elasticsearch, MinIO, RabbitMQ, OpenTelemetry, Keycloak, backend services, and GUI containers.
- Docker image build/push behavior is controlled through Maven properties.

Developers can run targeted module builds with `-pl <module> -am` and disable Docker work with:

```bash
-Ddocker.skip.build=true -Ddocker.skip.push=true
```

## 7. Observability by Default in Services

Runnable backend modules include standard logging and OpenTelemetry dependencies when they emit runtime telemetry. Libraries and DTO modules avoid runtime observability dependencies.

## 8. Test Scope Matches Risk

Tests are kept close to the behavior being changed:

- Data modules test parsing, catalog alignment, and invariants.
- Mapping modules test A-to-B and B-to-A transformations.
- Service modules test controller validation, service orchestration, and adapter-facing behavior.
- GUI modules use TypeScript build and focused component tests.

## 9. Documentation Beside the Module

Each module owns a `README.md` explaining purpose, contents, implementation notes, and developer commands. Architecture documents link to those module READMEs instead of duplicating every detail.
