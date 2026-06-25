# Architecture Decisions Applied

## ADR 001: Shared Modules Are The Current Scope

The repository has been reduced to shared platform modules and architecture documentation. The current scope is utility/configuration, mapping, infrastructure, authentication, and vault support.

The former CGM import, data, mapping, BPM, service, and GUI implementation was removed from the build and documentation. Those modules may be reintroduced later with redesigned business logic.

## ADR 002: `com.*` Is The Shared Capability Boundary

Shared capabilities live in `com.*` modules. Current active modules are:

- `com.utils`
- `com.mapping`
- `com.infra`
- `com.auth`
- `com.vault`

The architecture documents should describe active `com.*` ownership and add domain module families only when their redesigned implementations are introduced.

## ADR 003: YAML Configuration Replaced XML Configuration

Application and infrastructure configuration uses YAML files loaded by `com.utils`.

Configuration is environment-aware and layered by module name. Base files provide defaults; environment folders override only changed values.

## ADR 004: Vault Resolution Is Authorized And Cycle-Free

Vault support is implemented in `com.vault`.

`com.vault` resolves `${vault:KEY}` placeholders after `com.utils` loads module configuration. Every secret lookup checks authorization through `com.utils.secret.SecretAuthorizationService`.

`com.auth` remains independent from `com.vault`, avoiding a dependency cycle.

## ADR 005: Infrastructure Uses Adapter Boundaries

`com.infra` hides storage, messaging, object storage, and BPM technologies behind interfaces.

Applications should use `InfrastructureUtils` and shared interfaces rather than importing MinIO, Elasticsearch, RabbitMQ, or Camunda implementation classes directly.

## ADR 006: MinIO Bucket Creation Is Not In The Concurrent Upload Path

Bucket initialization belongs in startup/initialization logic.

The object upload path should assume buckets are already initialized and should only persist the object payload. Already-owned bucket responses during initialization are treated as successful.

## ADR 007: BPM Support Remains Generic

`com.infra` may expose generic BPM contracts and Camunda/remote adapters.

Concrete process identifiers and routes must not leak into generic BPM code, remote paths, tests, or documentation. Process-specific details belong in the process module that owns them.

## ADR 008: CGM Modules May Be Reintroduced With Redesigned Logic

The following module names may be reused for a redesigned implementation:

- `bpm.cgm.import`
- `data.cgm`
- `gui.cgm.explorer`
- `map.cgm`
- `srv.cgm.importer`

Future implementations should use the shared `com.*` capabilities for configuration, mapping, infrastructure, authentication, and vault access. Do not copy back the removed implementation by default; rebuild the business logic according to the new design requested at that time.

## ADR 009: OpenTelemetry Collector Uses Debug Exporter Locally

The local collector configuration keeps a health check endpoint and uses the debug exporter for local trace output. Avoid reintroducing deprecated exporter names.

## ADR 010: Maven Cache Is Not Source

`work/m2` is a local Maven repository cache. Do not stage or commit changes under it.

## ADR 011: CNM Import Starts With Contracted Import Metadata

The first redesigned CNM module family is:

- `data.cnm`
- `srv.cnm.services`
- `mock.srv.cnm.services`
- `gui.common`
- `gui.cnm.manager`

The initial increment imports RDF profile files, detects CGMES/NCP profile references from RDF metadata, stores raw payloads through `com.infra`, and records import metadata. Deeper semantic graph loading, validation, PowSyBl-backed IIDM transformation, CSA, and CC workflows remain future increments.

## ADR 012: Shared Vue Components Live In `gui.common`

Standard Vue components and common styling are isolated in `gui.common`. CNM GUI modules consume those components instead of duplicating table, button, link, menu, dropdown, search, sort, and pagination behavior.

## ADR 013: CNM Mock Service Follows The OpenAPI Shape

`mock.srv.cnm.services` exists to unblock frontend development and must mirror the REST shape exposed by `srv.cnm.services`. It should use in-memory responses and avoid production infrastructure dependencies.

## ADR 014: CNM Upload Failures Are Durable And Retryable

The GUI creates an import ID before uploading model bytes. Proxy, network, multipart, parsing, and storage failures must result in a persisted `FAILED` import whenever the document repository remains reachable.

Re-upload uses the same import ID and replaces the failed status through `INIT` and the final per-file state. The GUI proxy and Spring multipart limits must be configured consistently for large model bundles.

## ADR 015: Large CNM Files Use Bounded Chunks

The GUI sends files up to 1 GB as 8 MB `application/octet-stream` chunks. The
service stages and validates chunks before invoking import processing. Proxy and
Spring request limits remain bounded at 16 MB per request.

Filename metadata is authoritative for profile type, TSO, business day,
business time, timeframe, and version. Successful imports persist searchable
profile documents and publish a metadata event through `com.infra`.

## ADR 016: REST Service Support Is Shared

Generic REST-facing service support belongs in `com.utils.restservice`.
`RestServiceSupport` owns shared logging, environment, module-name, and
observation-registry access. Runnable applications explicitly import
`RestServiceConfiguration` when they need the shared timeout-configured
`RestTemplate`.

## ADR 017: Codex Guidance Lives Under `doc.arch`

Repository-specific Codex rules, workflow guidance, and applied architecture
decisions live in `doc.arch/codegen/codex`. Future requests must read that
directory before changing code or documentation.

## ADR 018: CNM Import Status Separates Aggregate And File Metadata

The import aggregate exposes only `INIT`, `STORED`, and `FAILED`. Profile
classification belongs to each imported file rather than the aggregate row.
The GUI therefore links from the aggregate File column to a searchable,
sortable, paginated file-detail view.

An optional user message is accepted with the upload completion request and
persisted on `ImportStatus`. Day-ahead values use `DAY_AHEAD` in transport
contracts and display as `DAY AHEAD`.

## ADR 019: File And Aggregate Import Lifecycles Are Separate

`ImportState` is aggregate-only and contains `INIT`, `STORED`, and `FAILED`.
`ImportFileState` contains `INIT`, `STORED`, `PARSED`, and `FAILED`.

Downstream processors report file progress through the CNM file-status update
contract. The service persists the file state and derives the parent state:
any failed file produces `FAILED`, any initialized file produces `INIT`, and
otherwise the aggregate remains `STORED`.

## ADR 020: Elasticsearch Documents Store Primitive Timestamps

Application Elasticsearch documents write timestamps as epoch milliseconds and
read them through schema-tolerant document fields. API DTOs normalize numeric,
numeric-string, ISO, and legacy `Instant` values at the service boundary. This
avoids converter registration requirements for nested records.
