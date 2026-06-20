# Module Classification

This document classifies project modules and links to module READMEs for details.

## `com.*` Modules

`com.*` modules provide reusable cross-cutting capabilities. They should not contain domain-specific workflow logic unless the capability itself is shared across services.

Current modules:

- `com.env`: environment resolution. See [README](../com.env/README.md).
- `com.utils`: shared utilities and cache abstractions. See [README](../com.utils/README.md).
- `com.app.config`: module configuration loading. See [README](../com.app.config/README.md).
- `com.infra`: infrastructure adapters for document storage, object storage, and messaging. See [README](../com.infra/README.md).
- `com.auth`: authentication and authorization services. See [README](../com.auth/README.md).
- `com.mapping`: generic object mapping service. See [README](../com.mapping/README.md).

Rules:

- Keep APIs generic.
- Hide technology dependencies behind services/adapters.
- Avoid importing service modules.

## `data.*` Modules

`data.*` modules define stable DTOs, enums, validation rules, and domain vocabulary.

Current modules:

- `data.cgmes`: CGMES DTOs and PowSyBl CGMES model alignment. See [README](../data.cgmes/README.md).
- `data.iidm`: IIDM DTOs and PowSyBl IIDM extension alignment. See [README](../data.iidm/README.md).

Rules:

- No Spring MVC, persistence, RabbitMQ, MinIO, or Elasticsearch dependencies.
- PowSyBl contract dependencies are allowed when they define the data vocabulary.
- Keep runtime behavior out of data modules.

## `map.*` Modules

`map.*` modules transform between data models.

Current modules:

- `map.cgmes.iidm`: CGMES-to-IIDM and IIDM-to-CGMES transformations. See [README](../map.cgmes.iidm/README.md).

Rules:

- Depend on source and target `data.*` modules.
- Use `com.mapping.MappingService` for generic field transfer.
- Keep technology adapters and API orchestration outside mapping modules.

## `srv.*` Modules

`srv.*` modules are backend services. They expose APIs, orchestrate workflows, apply validation, call infrastructure adapters, and publish domain events.

Current modules:

- `srv.cgm.importer`: CGMES import, indexing, search, comparison, and IIDM projection API. See [README](../srv.cgm.importer/README.md).

Rules:

- Use Java package `eu.egm.srv.<domain>.<capability>`.
- Depend only on required `data`, `map`, `com.app.config`, and `com.infra` modules.
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
