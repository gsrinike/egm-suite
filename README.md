# Energy Grid Management Suite

Open-source baseline for importing CGMES network data, indexing it for fast exploration, and comparing network states. The architecture follows the GridSuite direction: separate web, service, data, mapping, and infrastructure modules for operating, visualizing, analysing, and designing electrical grids on top of PowSyBl-oriented network import boundaries.

GridSuite references used for alignment: import/explore, interactive study, and modification analysis are key features, and its repositories include separate UI/server concerns such as `gridexplore-app`, `gridstudy-app`, `network-modification-server`, and `study-server` ([GridSuite GitHub](https://github.com/gridsuite)).

## Module Map

This README is the suite entry point. Detailed behavior belongs in each module README.

- `com.env`: runtime environment resolution.
- `com.utils`: shared utility code and cache abstractions.
- `com.app.config`: module-scoped configuration loading.
- `data.cgmes`: shared CGMES DTOs, constants, classification, and filename parsing.
- `data.iidm`: shared IIDM-facing network and equipment DTOs.
- `com.mapping`: generic configuration-driven object mapping.
- `map.cgmes.iidm`: CGMES-to-IIDM and IIDM-to-CGMES transformers.
- `com.infra`: reusable backend infrastructure adapters for document storage, object storage, and event publishing.
- `com.auth`: OIDC/OAuth 2.0 authorization service backed by Keycloak.
- `srv.cgm.importer`: Spring Boot REST service for CGMES upload, indexing, import history, and comparison.
- `gui.cgm.explorer`: React application for import, search/filter/navigation, and state comparison.
- `doc.arch`: architecture notes, local deployment details, module classification, and the module archetype.

## Build Metadata

- `dependencies.xml` is the Maven parent dependency catalog. It centralizes dependency and plugin versions through Maven-compatible `version.*` property tags, `dependencyManagement`, and `pluginManagement`; it does not add those dependencies to every module.
- `modules.xml` is the standalone module inventory for developer review and automation. Maven requires the active aggregator module list to remain inline in `pom.xml`, so keep both lists synchronized when adding or removing modules.
- Module POMs should declare only the dependencies they directly use. Infrastructure dependencies such as MinIO, Elasticsearch, RabbitMQ, and Spring Boot belong in backend modules that need them, not in the GUI or data-only modules.

## Quick Start

Start the shared local infrastructure:

```bash
docker compose -f docker/docker-compose.yml up elasticsearch minio rabbitmq otel-collector
```

Run the backend:

```bash
mvn -pl srv.cgm.importer -am spring-boot:run
```

Run the frontend:

```bash
cd gui.cgm.explorer
npm install
npm run dev
```

Open `http://localhost:5173`.

## Common Builds

Build all Maven modules:

```bash
mvn verify
```

Build only the GUI through Maven:

```bash
mvn -pl gui.cgm.explorer package
```

Build Docker images through Maven:

```bash
mvn -Ddocker.namespace=your-dockerhub-user package
```

Run the full local Docker Compose stack from locally built artifacts:

```bash
mvn -Ddocker.skip.build=true -Ddocker.skip.push=true clean package
docker compose -f docker/docker-compose.yml up
```

## Where Details Live

- Backend APIs, import behavior, filename rules, runtime dependencies, and backend Docker build commands: `srv.cgm.importer/README.md`.
- Frontend structure, Vite/npm commands, API client usage, and GUI Docker image details: `gui.cgm.explorer/README.md`.
- Authentication endpoints, Keycloak configuration, and gateway authorization flow: `com.auth/README.md`.
- Configuration loading order and cache-provider resolution: `com.app.config/README.md`.
- Infrastructure adapter contracts and technology ownership rules: `com.infra/README.md`.
- CGMES and IIDM data contracts: `data.cgmes/README.md` and `data.iidm/README.md`.
- Mapping implementation details: `com.mapping/README.md` and `map.cgmes.iidm/README.md`.
- Local deployment, environment rules, and architecture guidance: `doc.arch/README.md` and the documents under `doc.arch`.

## Tests

```bash
mvn test
mvn -pl gui.cgm.explorer package
```
