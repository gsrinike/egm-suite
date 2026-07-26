# Architecture Documentation

`doc.arch` is the architecture reference for the current EGM Suite codebase. It
describes the system as it exists now: shared capabilities, CNM import, RDF
metadata management, IIDM transformation, RCC common analysis screens, local
deployment, and repository rules.

## Documents

- `DESIGN_PRINCIPLES.md`: ownership, dependency, configuration, storage, and
  observability principles.
- `MODULE_CLASSIFICATION.md`: current module families and their responsibilities.
- `CNM_IMPORT_DESIGN.md`: CNM upload, object storage, asynchronous RDF
  processing, status lifecycle, and event publication.
- `RDF_METADATA_MGMT.md`: RDF4J streaming extraction, profile DTO payloads,
  Elasticsearch persistence, and dynamic profile tables.
- `IIDM_TRANSFORMATION_DESIGN.md`: PowSyBl CGMES-to-IIDM transformation,
  boundary data handling, diagnostics, and IIDM document/table storage.
- `RCC_CSA_DESIGN.md`: RCC manager, CGM load-flow/security-analysis and
  sensitivity-analysis flows, and common LF/SA service ownership.
- `LOCAL_DEPLOYMENT_AND_ENVIRONMENT.md`: Maven, Docker Compose, local
  infrastructure, environment selection, and YAML configuration layout.
- `codegen/codex`: repository rules and workflow guidance that apply to future
  Codex changes.
- `../gui.common/COMPONENTS.md`: reusable Vue component reference.

## Module Archetype

The archetype under `egm-module-archetype` creates a minimal Java module
skeleton. After generation, add the new module to both root `pom.xml` and
`modules.yml`, then declare only the dependencies the module directly uses.

```bash
mvn -f doc.arch/egm-module-archetype/pom.xml clean install
```
