# Module Classification

The repository is organized by module prefix. The prefix describes ownership and
allowed dependencies.

## Shared Capability Modules

- `com.utils`: environment, YAML configuration, cache, secret authorization
  contracts, REST service base support, and shared `RestTemplate` wiring.
- `com.mapping`: mapping contracts, reflection mapping, transformer contracts,
  and JSON conversion.
- `com.infra`: infrastructure contracts and adapters for document storage,
  object storage, events, and BPM.
- `com.auth`: Keycloak/OIDC authentication and authorization service.
- `com.vault`: authorized Vault, environment, and config fallback secret lookup.

Shared modules are domain-neutral. They expose contracts and adapters used by
application services.

## CNM And IIDM Modules

- `data.cnm`: CNM import DTOs, CGMES/NCP profile vocabulary, profile payload
  DTOs, common topology DTOs, snapshots, and dynamic table contracts.
- `data.iidm`: PowSyBl-backed IIDM event, diagnostic, summary, network, and
  XIIDM helper DTOs.
- `map.cnm.iidm`: CGMES source to PowSyBl IIDM transformation and legacy CNM DTO
  mapping.
- `srv.cnm.services`: CNM import, RDF extraction, metadata persistence, status
  management, and IIDM event publication.
- `srv.iidm.transformer`: IIDM transform worker/API, PowSyBl conversion,
  diagnostics capture, and IIDM document persistence.
- `mock.srv.cnm.services`: CNM mock service.
- `gui.cnm.manager`: CNM import and exploration UI.

CNM services use `com.infra` for storage/events and keep raw CGMES/IIDM
conversion outside the import service boundary.

## RCC And Common Analysis Modules

- `data.common`: shared RCC/CSA/LFSA/RAO/BPM transport DTOs.
- `srv.common.lfsa`: common load-flow and security-analysis API.
- `mock.srv.common.lfsa`: LF/SA mock API.
- `srv.common.rao`: common remedial-action optimization API.
- `mock.srv.common.rao`: RAO mock API.
- `srv.csa.services`: CSA orchestration API.
- `mock.srv.csa.services`: CSA mock API.
- `bpm.csa.service`: Camunda CSA workflow runtime.
- `gui.rcc.manager`: RCC manager UI.

LF/SA and RAO stay outside CSA because CSA, capacity calculation, and
operational planning workflows can reuse them.

## GUI Module

- `gui.common`: reusable Vue components, CSS, theme persistence, refresh
  controls, and browser-side error logging.

Feature GUIs depend on `gui.common` and own their business views and API
integration.

## Maven Inventory

The root `pom.xml` and `modules.yml` are the active module inventory. Keep them
synchronized when modules are added, removed, or renamed.
