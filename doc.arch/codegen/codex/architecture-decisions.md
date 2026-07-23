# Architecture Decisions

This document records the current architecture decisions that guide future
changes. It is written as current state, not as chronological change history.

## Module Families

The active reactor contains shared `com.*` modules, CNM/IIDM modules,
RCC/common-analysis modules, BPM, mocks, and GUI modules. Root `pom.xml` and
`modules.yml` are the active inventory and must stay synchronized.

## Shared Capability Boundary

`com.*` modules provide reusable capabilities only:

- utility/configuration/cache/REST support in `com.utils`
- mapping and transformer contracts in `com.mapping`
- infrastructure adapters in `com.infra`
- authentication in `com.auth`
- authorized secret lookup in `com.vault`

Domain workflow logic belongs in application modules, not in `com.*`.

## Configuration And Secrets

All runtime configuration is YAML for backend modules. Configuration is
module-scoped, environment-aware, and loaded by `com.utils`.

Vault placeholders use `${vault:KEY}`. `com.vault` resolves them only after the
client/key pair is authorized by `com.utils.secret.SecretAuthorizationService`.
If Vault is not enabled, the same authorization check applies before environment
or config fallback values are returned.

## Infrastructure Adapters

`com.infra` owns technology adapters for Elasticsearch, MinIO, RabbitMQ, and
BPM. Application modules use `InfrastructureUtils` and storage/event/BPM
interfaces. Buckets and RabbitMQ exchanges are initialized at startup, outside
concurrent upload or publish hot paths.

## CNM Import

CNM import stores raw RDF/XML or ZIP payloads, persists import/file metadata,
processes RDF asynchronously, stores profile metadata and payload JSON, and
publishes IIDM transform requests after a complete model group is parsed.

Large uploads are chunked by the GUI. Profile type, TSO, business day, business
time, timeframe, and version are derived from model filenames and RDF metadata.

## RDF Metadata

RDF parsing uses RDF4J streaming through `CgmStreamingRdfHandler`. The extraction
layer writes lightweight searchable metadata separately from large JSON payloads.
Common topology and mRID data are reused across profile-specific DTOs and
snapshot assembly.

## IIDM Transformation

`srv.iidm.transformer` consumes transform events and preferably converts raw
CGMES source files directly through PowSyBl. Boundary files are staged with the
model group. The service persists transform documents, diagnostics, XIIDM
payloads, and GUI-oriented JSON table projections.

## RCC And CSA

CSA orchestration is separated from common analysis capabilities. LF/SA and RAO
are common services. `bpm.csa.service` owns the Camunda process. `srv.csa.services`
interacts with BPM through `com.infra` contracts or remote BPM endpoints.

## Frontend

Reusable Vue components and shared styling live in `gui.common`. Feature GUIs
own business screens and read environment-specific runtime API URLs from JSON
configuration before mounting.

## Local Development

Use Maven with `work/m2` for local dependency cache. Do not commit `work/m2`.
Use Docker Compose through `docker/egm-compose.sh` or `build-and-deploy.sh`.
The local OpenTelemetry collector uses supported local exporters only.
