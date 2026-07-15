# Module Classification

This document classifies current project modules and links to module READMEs for details.

## `com.*` Modules

`com.*` modules provide reusable cross-cutting capabilities. They should not contain domain-specific workflow logic unless the capability itself is shared across services.

Current modules:

- `com.utils`: shared utilities, cache abstractions, environment resolution, module configuration loading, REST service support, outbound `RestTemplate` configuration, and bootstrap secret access authorization contracts. See [README](../com.utils/README.md).
- `com.vault`: HashiCorp Vault and fallback secret lookup, including authorized `${vault:...}` configuration placeholders. Depends on `com.utils` for bootstrap client/key authorization. See [README](../com.vault/README.md).
- `com.infra`: infrastructure adapters for document storage, object storage, messaging, and BPM process orchestration. Document abstractions live under `com.infra.storage.document`, object storage under `com.infra.storage.object`, message adapters under `com.infra.event`, and BPM adapters under `com.infra.bpm`. Camunda-specific implementation classes live under `com.infra.bpm.camunda`. See [README](../com.infra/README.md).
- `com.auth`: authentication and runtime authorization services. It must not depend on `com.vault`. See [README](../com.auth/README.md).
- `com.mapping`: generic object mapping service. See [README](../com.mapping/README.md).

Rules:

- Keep APIs generic.
- Hide technology dependencies behind services/adapters.
- Avoid importing service modules.

## CNM Application Modules

`cnm` modules implement the Common Network Model application surface for CGMES/NCP based import.

Current modules:

- `data.cnm`: data transfer objects shared between CNM GUI, service, and mock modules. Packages are separated by `common`, `cgmes`, `ncp`, and `iidm`. See [README](../data.cnm/README.md).
- `srv.cnm.services`: Spring Boot REST service that accepts RDF profile files, classifies CGMES/NCP profile references, stores raw files through `com.infra`, and persists import metadata. See [README](../srv.cnm.services/README.md).
- `mock.srv.cnm.services`: mock REST service compatible with the CNM OpenAPI shape for GUI development without infrastructure. See [README](../mock.srv.cnm.services/README.md).
- `gui.common`: Vue shared component library for standard table, button, link, menu, and dropdown behavior. See [README](../gui.common/README.md).
- `gui.cnm.manager`: Vue CNM manager application for RDF upload and import status viewing. See [README](../gui.cnm.manager/README.md).

Rules:

- `srv.cnm.services` invokes infrastructure through `com.infra`.
- `gui.cnm.manager` consumes shared UI from `gui.common`.
- `mock.srv.cnm.services` follows the REST contract and must not own production infrastructure behavior.
- `data.cnm` remains transport-focused and does not depend on Spring, PowSyBl, Elasticsearch, MinIO, or RabbitMQ.

## RCC And Common Analysis Modules

`rcc` and CSA modules implement the Regional Coordination Centre workflow surface. Load flow, security analysis, and remedial-action optimization are common service capabilities because they are reused by CSA, capacity calculation, and operational planning workflows.

Current modules:

- `data.common`: transport DTOs shared by RCC, CSA, LF/SA, RAO, BPM, GUI, and mock modules. See [README](../data.common/README.md).
- `srv.common.lfsa`: Spring Boot REST service for load-flow and security-analysis execution. See [README](../srv.common.lfsa/README.md).
- `mock.srv.common.lfsa`: mock REST service aligned with the common LF/SA OpenAPI contract. See [README](../mock.srv.common.lfsa/README.md).
- `srv.common.rao`: Spring Boot REST service for remedial-action optimization execution. See [README](../srv.common.rao/README.md).
- `mock.srv.common.rao`: mock REST service aligned with the common RAO OpenAPI contract. See [README](../mock.srv.common.rao/README.md).
- `srv.csa.services`: Spring Boot REST service that starts CSA cases, invokes common LF/SA and RAO services, and delegates process start to BPM through `com.infra` BPM contracts. See [README](../srv.csa.services/README.md).
- `mock.srv.csa.services`: mock REST service aligned with the CSA OpenAPI contract. See [README](../mock.srv.csa.services/README.md).
- `bpm.csa.service`: Camunda-backed CSA process module exposed through process-neutral BPM REST endpoints. See [README](../bpm.csa.service/README.md).
- `gui.rcc.manager`: Vue RCC manager focused on CSA execution and workflow monitoring. See [README](../gui.rcc.manager/README.md).

Rules:

- CSA service modules must not depend directly on `bpm.csa.service`; they use `com.infra.bpm` interfaces or remote BPM REST endpoints.
- LF/SA and RAO service contracts stay outside CSA so CSA, CC, and OPC can share them.
- Mock modules mirror OpenAPI shapes and avoid production infrastructure ownership.
- `data.common` remains transport-focused and does not depend on Spring, Camunda, Elasticsearch, or grid-engine implementations.

## Naming Examples

- Common capability: `com.audit`
- CNM application module: `srv.cnm.services`
- CNM GUI module: `gui.cnm.manager`
- RCC common data module: `data.common`
- Common service module: `srv.common.lfsa`
- CSA service module: `srv.csa.services`
- CSA process module: `bpm.csa.service`
- RCC GUI module: `gui.rcc.manager`

All Maven modules use group id `eu.egm`.
