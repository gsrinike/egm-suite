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

`cnm` modules implement the Common Network Model application surface for CGMES/NCP based import and future CGM, CSA, and CC workflows.

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

## Naming Examples

- Common capability: `com.audit`
- CNM application module: `srv.cnm.services`
- CNM GUI module: `gui.cnm.manager`

All Maven modules use group id `eu.egm`.
