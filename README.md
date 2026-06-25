# Energy Grid Management Suite

Open-source baseline for shared Energy Grid Management platform capabilities. The current repository contains reusable utility, mapping, infrastructure, authentication, and vault modules plus the first Common Network Model (CNM) import application modules.

## Module Map

This README is the suite entry point. Detailed behavior belongs in each module README.

- `com.utils`: shared utility code, cache abstractions, environment resolution, and module-scoped configuration loading.
- `com.mapping`: generic configuration-driven object mapping.
- `com.infra`: reusable backend infrastructure adapters for document storage, object storage, event publishing, and BPM process invocation.
- `com.auth`: OIDC/OAuth 2.0 authorization service backed by Keycloak.
- `com.vault`: HashiCorp Vault and fallback secret lookup with bootstrap authorization checks.
- `data.cnm`: DTOs exchanged by CNM GUI, service, and mock modules for CGMES, NCP, and IIDM metadata.
- `srv.cnm.services`: Spring Boot REST service for CNM RDF import and import status retrieval.
- `mock.srv.cnm.services`: mock Spring Boot service compatible with the CNM import OpenAPI contract.
- `gui.common`: Vue shared component and styling module for standard buttons, links, menus, dropdowns, and data tables.
- `gui.cnm.manager`: Vue CNM manager application for importing RDF models and viewing upload status.
- `doc.arch`: architecture notes, local deployment details, module classification, and the module archetype.

## Build Metadata

- `dependencies.xml` is the Maven parent dependency catalog. It centralizes dependency and plugin versions through Maven-compatible `version.*` property tags, `dependencyManagement`, and `pluginManagement`; it does not add those dependencies to every module.
- `modules.yml` is the standalone module inventory for developer review and automation. Maven requires the active aggregator module list to remain inline in `pom.xml`, so keep both lists synchronized when adding or removing modules.
- Module POMs should declare only the dependencies they directly use. Infrastructure dependencies such as MinIO, Elasticsearch, RabbitMQ, Camunda, and Spring Boot belong in backend modules that need them.

## Quick Start

Start the shared local infrastructure:

```bash
docker compose -f docker/docker-compose.yml up elasticsearch minio rabbitmq keycloak otel-collector
```

Run the CNM service:

```bash
mvn -pl srv.cnm.services -am spring-boot:run
```

## Common Builds

Build all Maven modules:

```bash
mvn verify
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

- Authentication endpoints, Keycloak configuration, and gateway authorization flow: `com.auth/README.md`.
- Configuration loading order, environment resolution, and cache-provider resolution: `com.utils/README.md`.
- Infrastructure adapter contracts and technology ownership rules: `com.infra/README.md`.
- Mapping implementation details: `com.mapping/README.md`.
- Vault configuration and authorized secret resolution: `com.vault/README.md`.
- CNM import application details: `data.cnm/README.md`, `srv.cnm.services/README.md`, `mock.srv.cnm.services/README.md`, `gui.common/README.md`, and `gui.cnm.manager/README.md`.
- Local deployment, environment rules, and architecture guidance: `doc.arch/README.md` and the documents under `doc.arch`.

## Tests

```bash
mvn test
```
