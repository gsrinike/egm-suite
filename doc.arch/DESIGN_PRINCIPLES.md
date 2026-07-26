# Design Principles

This document describes the principles currently applied in the repository.

## Modular Ownership

Each module owns one kind of responsibility.

- `com.*` modules own shared platform capabilities: configuration, mapping,
  infrastructure adapters, authentication, vault integration, cache helpers, and
  REST service utilities.
- `data.*` modules own transport contracts and DTOs.
- `map.*` modules own transformation logic without Spring or infrastructure
  dependencies.
- `srv.*` modules own runnable REST services and workers.
- `mock.srv.*` modules mirror service contracts with deterministic in-memory
  behavior.
- `bpm.*` modules own process definitions and process runtime endpoints when a
  workflow runtime module is active.
- `gui.*` modules own Vue applications or shared Vue components.

## Dependency Direction

Dependencies point from applications toward stable shared contracts.

- Service modules use `com.utils`, `com.infra`, `com.mapping`, and data modules.
- Services invoke infrastructure through `InfrastructureUtils`; they do not
  import Elasticsearch, MinIO, RabbitMQ, or Camunda adapters directly.
- `srv.cnm.services` publishes IIDM events but does not depend on
  `srv.iidm.transformer`.
- `srv.iidm.transformer` owns IIDM persistence and may use PowSyBl through
  `data.iidm` and `map.cnm.iidm`.
- Feature orchestration uses common LF/SA contracts instead of embedding
  reusable analysis logic in GUI or workflow modules.
- `com.vault` depends on `com.utils` for bootstrap secret authorization.
  `com.auth` does not depend on `com.vault`.

## Configuration

Runtime configuration is YAML-based and module-scoped. `com.utils` resolves the
runtime environment from JVM property `env`, environment variable `ENV`, then
`local`.

Configuration files follow this shape:

- `base/<module>-application.yml`
- `base/<module>-infra.yml`
- `base/<module>-cache-config.yml`
- `base/<module>-vault.yml`
- `<env>/<module>-application.yml`
- `<env>/<module>-infra.yml`
- `<env>/<module>-cache-config.yml`
- `<env>/<module>-vault.yml`

Environment-specific files override base defaults. Secrets can be referenced as
`${vault:KEY}` and are authorized before Vault, environment, or config fallback
values are returned.

## Storage And Events

Document storage is separated by ownership and query use:

- CNM import metadata, profile metadata, profile payloads, profile fragments,
  mRID indexes, and snapshots are owned by `srv.cnm.services`.
- IIDM transform states and IIDM networks are owned by `srv.iidm.transformer`.
- Large JSON/XML payloads are chunked or stored separately from list/search
  documents so list screens do not load heavy data.
- RabbitMQ topic exchanges are declared at application startup by publishers.
- CNM and IIDM queues retry failed listener handling three times and route
  exhausted messages to module-owned dead-letter queues.

## Frontend Reuse

`gui.common` owns shared styling, theme utilities, browser logging, refresh
controls, and reusable components such as `DataTable` and `DynamicTable`.
Feature GUIs consume these components through the package entry point.

## Verification

Material code changes should be verified with targeted Maven or npm commands.
Documentation-only changes should be validated with reference scans for stale
module names, broken links, and contradictory state descriptions.
