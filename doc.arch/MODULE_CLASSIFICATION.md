# Module Classification

This document classifies project modules and links to module READMEs for details.

## `com.*` Modules

`com.*` modules provide reusable cross-cutting capabilities. They should not contain domain-specific workflow logic unless the capability itself is shared across services.

Current modules:

- `com.utils`: shared utilities, cache abstractions, environment resolution, and module configuration loading. See [README](../com.utils/README.md).
- `com.vault`: HashiCorp Vault and fallback secret lookup, including authorized `${vault:...}` configuration placeholders. Depends on `com.auth` for secret-key authorization. See [README](../com.vault/README.md).
- `com.infra`: infrastructure adapters for document storage, object storage, and messaging. Document abstractions live under `com.infra.storage.document`, object storage under `com.infra.storage.object`, and message adapters under `com.infra.event`. See [README](../com.infra/README.md).
- `com.auth`: authentication, authorization, and secret-access authorization policies. It must not depend on `com.vault`. See [README](../com.auth/README.md).
- `com.mapping`: generic object mapping service. See [README](../com.mapping/README.md).

Rules:

- Keep APIs generic.
- Hide technology dependencies behind services/adapters.
- Avoid importing service modules.

## `data.*` Modules

`data.*` modules define stable DTOs, enums, validation rules, and domain vocabulary.

Current modules:

- `data.cgm`: CGMES and IIDM DTOs, CGMES profile loading, PowSyBl-backed reading, fallback graph projection, and topology strategies. See [README](../data.cgm/README.md).

Rules:

- No Spring MVC, persistence, RabbitMQ, MinIO, or Elasticsearch dependencies.
- PowSyBl dependencies are isolated in `data.cgm` for CGM import/projection behavior.
- Keep storage, messaging, web, and UI behavior out of data modules.

## `map.*` Modules

`map.*` modules transform between data models.

Current modules:

- `map.cgm`: CGMES-to-IIDM and IIDM-to-CGMES transformations. See [README](../map.cgm/README.md).

Rules:

- Depend on source and target `data.*` modules.
- Use `eu.egm.mapping.MappingService` for generic field transfer.
- Keep technology adapters and API orchestration outside mapping modules.

## `srv.*` Modules

`srv.*` modules are backend services. They expose APIs, orchestrate workflows, apply validation, call infrastructure adapters, and publish domain events.

Current modules:

- `srv.cgm.importer`: CGMES import, indexing, search, comparison, and IIDM projection API. See [README](../srv.cgm.importer/README.md).

Rules:

- Use Java package `eu.egm.srv.<domain>.<capability>`.
- Depend only on required `data`, `map`, `com.utils`, and `com.infra` modules. `srv.cgm.importer` depends on `data.cgm` for CGM data behavior and does not import PowSyBl directly.
- Use Spring Boot only in runnable service modules.
- Use standard logging and OpenTelemetry where runtime telemetry is emitted.

## `gui.*` Modules

`gui.*` modules are frontend applications wrapped by Maven for consistent build and Docker lifecycle.

Current modules:

- `gui.cgm.explorer`: React explorer for import, filtering, comparison, and IIDM visualization. See [README](../gui.cgm.explorer/README.md).

Rules:

- Use HTTP API contracts rather than importing Java code.
- Keep UI workflows feature-complete and responsive.
- Use Maven only as the wrapper for npm build/test/package and Docker lifecycle.

## Naming Examples

- Common capability: `com.audit`
- Data model: `data.market`
- Mapping module: `map.market.iidm`
- Backend service: `srv.cgm.analysis`
- Frontend app: `gui.cgm.analysis`

All Maven modules use group id `eu.egm`.
