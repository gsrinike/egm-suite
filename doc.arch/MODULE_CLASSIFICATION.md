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

- `data.common`: shared RCC and LF/SA transport DTOs, organized by capability:
  `lfsa.common` for load-flow/security-analysis contracts and `lfsa.sensitivity`
  for sensitivity-analysis contracts.
- `srv.common.lfsa`: common load-flow, security-analysis, and
  sensitivity-analysis API.
- `mock.srv.common.lfsa`: LF/SA mock API.
- `gui.rcc.manager`: RCC manager UI.

LF/SA stays outside feature-specific workflows because CSA, capacity
calculation, and operational planning workflows can reuse the same contracts.

## GUI Module

- `gui.common`: reusable Vue components, CSS, theme persistence, refresh
  controls, and browser-side error logging.

Feature GUIs depend on `gui.common` and own their business views and API
integration.

## Maven Inventory

The root `pom.xml` and `modules.yml` are the active module inventory. Keep them
synchronized when modules are added, removed, or renamed.
