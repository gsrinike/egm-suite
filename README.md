# Energy Grid Management Suite

Energy Grid Management Suite is a Maven multi-module workspace for grid model
management and regional coordination applications. The current codebase provides
shared platform capabilities, Common Network Model import, PowSyBl IIDM
transformation, RCC/CSA orchestration, reusable analysis service boundaries, and
Vue-based management screens.

## Current Module Families

### Shared capabilities

- `com.utils`: environment resolution, YAML configuration loading, cache helpers,
  REST service support, outbound `RestTemplate` configuration, and bootstrap
  secret authorization contracts.
- `com.mapping`: generic mapping contracts, reflection mapping, transformer
  contracts, and JSON conversion.
- `com.infra`: infrastructure adapters for Elasticsearch document storage,
  MinIO object storage, RabbitMQ events, and Camunda/remote BPM integration.
- `com.auth`: Keycloak-backed OIDC/OAuth2 authentication and authorization
  service.
- `com.vault`: authorized Vault/environment/config secret resolution.

### CNM and IIDM

- `data.cnm`: transport DTOs for CNM import, CGMES/NCP profile metadata,
  profile payloads, common topology, snapshots, and dynamic tables.
- `data.iidm`: PowSyBl-backed IIDM event and network DTOs.
- `map.cnm.iidm`: CGMES source and compatibility CNM DTO to PowSyBl IIDM
  transformation.
- `srv.cnm.services`: CNM import service for chunked uploads, raw object
  storage, RDF metadata extraction, profile persistence, and IIDM event
  publication.
- `srv.iidm.transformer`: IIDM transformer service that consumes transform
  events, converts raw CGMES groups with PowSyBl, and persists IIDM documents.
- `mock.srv.cnm.services`: in-memory CNM mock service.
- `gui.common`: shared Vue components, styling, theme utilities, and browser
  logging helpers.
- `gui.cnm.manager`: CNM import and exploration UI.

### RCC, CSA, and common analysis

- `data.common`: DTOs shared by RCC, CSA, load-flow/security-analysis, RAO,
  BPM, GUI, and mocks.
- `srv.common.lfsa`: reusable load-flow and security-analysis service boundary.
- `mock.srv.common.lfsa`: mock LF/SA service.
- `srv.common.rao`: reusable remedial-action optimization service boundary.
- `mock.srv.common.rao`: mock RAO service.
- `srv.csa.services`: CSA orchestration service.
- `mock.srv.csa.services`: mock CSA orchestration service.
- `bpm.csa.service`: Camunda-backed CSA workflow service.
- `gui.rcc.manager`: RCC manager UI with CGM import integration, CSA screens,
  and workflow monitoring.

## Build And Run

Build all Maven modules:

```bash
mvn -Dmaven.repo.local=work/m2 verify
```

Package selected backend modules without Docker work:

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.cnm.services,srv.iidm.transformer -am package
```

Start the local stack:

```bash
docker/egm-compose.sh up
```

Build and deploy the main local CNM/RCC stack:

```bash
./build-and-deploy.sh
```

Run Vue modules locally from their module directory:

```bash
npm install
npm run dev
```

## Documentation

- Architecture overview: `doc.arch/README.md`
- Current design principles: `doc.arch/DESIGN_PRINCIPLES.md`
- Module classification: `doc.arch/MODULE_CLASSIFICATION.md`
- CNM import design: `doc.arch/CNM_IMPORT_DESIGN.md`
- RDF metadata design: `doc.arch/RDF_METADATA_MGMT.md`
- IIDM transformation design: `doc.arch/IIDM_TRANSFORMATION_DESIGN.md`
- RCC/CSA design: `doc.arch/RCC_CSA_DESIGN.md`
- Local deployment and environment: `doc.arch/LOCAL_DEPLOYMENT_AND_ENVIRONMENT.md`
- Codex/repository rules: `doc.arch/codegen/codex/README.md`

Each active module also owns a local `README.md` with its scope, package shape,
and developer commands.
