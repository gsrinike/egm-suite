# Module Classification

This document classifies current project modules and links to module READMEs for details.

## `com.*` Modules

`com.*` modules provide reusable cross-cutting capabilities. They should not contain domain-specific workflow logic unless the capability itself is shared across services.

Current modules:

- `com.utils`: shared utilities, cache abstractions, environment resolution, module configuration loading, and bootstrap secret access authorization contracts. See [README](../com.utils/README.md).
- `com.vault`: HashiCorp Vault and fallback secret lookup, including authorized `${vault:...}` configuration placeholders. Depends on `com.utils` for bootstrap client/key authorization. See [README](../com.vault/README.md).
- `com.infra`: infrastructure adapters for document storage, object storage, messaging, and BPM process orchestration. Document abstractions live under `com.infra.storage.document`, object storage under `com.infra.storage.object`, message adapters under `com.infra.event`, and BPM adapters under `com.infra.bpm`. Camunda-specific implementation classes live under `com.infra.bpm.camunda`. See [README](../com.infra/README.md).
- `com.auth`: authentication and runtime authorization services. It must not depend on `com.vault`. See [README](../com.auth/README.md).
- `com.mapping`: generic object mapping service. See [README](../com.mapping/README.md).

Rules:

- Keep APIs generic.
- Hide technology dependencies behind services/adapters.
- Avoid importing service modules.

## Naming Examples

- Common capability: `com.audit`

All Maven modules use group id `eu.egm`.
