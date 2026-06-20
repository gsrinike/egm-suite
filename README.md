# Energy Grid Management Suite

Open-source baseline for importing CGMES network data, indexing it for fast exploration, and comparing network states. The architecture follows the GridSuite direction: web applications for operating, visualizing, analysing, and designing electrical grids on top of PowSyBl-oriented network import boundaries.

GridSuite references used for alignment: the project describes import/explore, interactive study, and modification analysis as key features, and its repositories include separate UI/server concerns such as `gridexplore-app`, `gridstudy-app`, `network-modification-server`, and `study-server` ([GridSuite GitHub](https://github.com/gridsuite)).

## Approach

The suite is split into independently deployable and testable modules:

- `com.env`: exposes `EnvironmentResolverService`, which resolves the runtime environment from `env` or `ENV`, defaulting to `local`.
- `com.utils`: shared utility code, including configurable cache abstractions used by configuration loading.
- `com.app.config`: loads module-scoped base, infrastructure, cache, and environment-specific configuration.
- `data.cgmes`: shared DTOs, constants, equipment classification, CGMES filename parsing, and PowSyBl CGMES model alignment.
- `data.iidm`: shared IIDM-facing network/equipment DTOs and PowSyBl IIDM extension alignment.
- `com.mapping`: generic configuration-driven object mapping service for A-to-B and B-to-A transformations.
- `map.cgmes.iidm`: CGMES-to-IIDM and IIDM-to-CGMES transformers backed by `MappingService`.
- `com.infra`: reusable backend infrastructure adapters for document storage, object storage, and event publishing.
- `com.auth`: OIDC/OAuth 2.0 authorization service backed by Keycloak, with gateway checks and admin management APIs.
- `srv.cgm.importer`: Spring Boot REST service for CGMES upload, raw storage, searchable indexing, and network comparison.
- `gui.cgm.explorer`: React application for import, search/filter/navigation, and state comparison.

The import flow stores the raw file in MinIO, parses searchable network equipment through `CgmesNetworkReader`, indexes `EquipmentDocument` records in Elasticsearch, and publishes an import event through RabbitMQ. The current reader is isolated behind a PowSyBl-facing adapter so a production integration can replace the lightweight RDF/XML extraction with PowSyBl IIDM/CGMES import calls without changing controllers, persistence, or GUI contracts. Mapping between CGMES projections and IIDM projections is handled in `map.cgmes.iidm`, while reusable field mapping mechanics remain in `com.mapping`.

## Build Metadata

- `dependencies.xml` is the Maven parent dependency catalog. It centralizes dependency and plugin versions through Maven-compatible `version.*` property tags, `dependencyManagement`, and `pluginManagement`; it does not add those dependencies to every module.
- `modules.xml` is the standalone module inventory for developer review and automation. Maven requires the active aggregator module list to remain inline in `pom.xml`, so keep both lists synchronized when adding or removing modules.
- Module POMs should declare only the dependencies they directly use. Infrastructure dependencies such as MinIO, Elasticsearch, RabbitMQ, and Spring Boot belong in backend modules that need them, not in the GUI or data-only modules.

## Configuration

Spring Boot service modules no longer keep runtime values in `application.yml`. Configuration is loaded by `com.app.config` from module-specific resources organized by environment folder:

- `base/<module>-application.xml`
- `base/<module>-infra.xml`
- `base/<module>-cache-config.yml`
- `${env}/<module>-application.xml`
- `${env}/<module>-infra.xml`
- `${env}/<module>-cache-config.yml`

The `env` value is resolved by `EnvironmentResolverService` from the JVM property `env`, then `ENV`, and defaults to `local`. Base files are loaded first and environment-specific files override them. Cache behavior is configured through `<module>-cache-config.yml` files and resolved by `CacheConfigurationService`; the current provider values are `java` and `none`.

## Run Locally

Start infrastructure:

```bash
docker compose -f docker/docker-compose.yml up elasticsearch minio rabbitmq otel-collector
```

Run backend:

```bash
mvn -pl srv.cgm.importer -am spring-boot:run
```

Run frontend:

```bash
cd gui.cgm.explorer
npm install
npm run dev
```

Open `http://localhost:5173`.

Build all modules with Maven:

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

Publish Docker images to Docker Hub during `install` or `deploy`:

```bash
docker login
mvn -Ddocker.namespace=your-dockerhub-user clean install
mvn -Ddocker.namespace=your-dockerhub-user deploy
```

The Maven Docker lifecycle builds and publishes:

- `docker.io/${docker.namespace}/egm-com-auth:0.1.0-SNAPSHOT`
- `docker.io/${docker.namespace}/cgm-srv-cgm-importer:0.1.0-SNAPSHOT`
- `docker.io/${docker.namespace}/cgm-gui-cgm-explorer:0.1.0-SNAPSHOT`
- `latest` tags for all images

Docker behavior is controlled by root `pom.xml` properties: `docker.registry`, `docker.namespace`, `docker.image.tag`, `docker.image.latest-tag`, `docker.skip.build`, and `docker.skip.push`. Maven artifact deployment is skipped by default with `maven.deploy.skip=true`, so `deploy` is used here for Docker image publication.

## Run Docker Images

The application containers depend on Elasticsearch, MinIO, RabbitMQ, and the OpenTelemetry collector. The simplest local path is Docker Compose:

```bash
mvn -Ddocker.skip.build=true -Ddocker.skip.push=true clean package
docker compose -f docker/docker-compose.yml up
```

The Maven command builds `srv.cgm.importer/target/srv.cgm.importer-0.1.0-SNAPSHOT.jar` and `gui.cgm.explorer/dist`. Compose then builds the service images from those local artifacts and starts the dependencies.

This starts:

- GUI: `http://localhost:5173`
- Auth service: `http://localhost:8082`
- Backend API: `http://localhost:8080`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`
- Auth OpenAPI UI: `http://localhost:8082/swagger-ui.html`
- Keycloak: `http://localhost:8081`
- Elasticsearch: `http://localhost:9200`
- MinIO API: `http://localhost:9000`
- MinIO console: `http://localhost:9001`
- RabbitMQ management: `http://localhost:15672`
- OpenTelemetry HTTP receiver: `http://localhost:4318`

To run already published images manually, first create a shared Docker network:

```bash
docker network create egm-suite
```

Start the dependencies:

```bash
docker run -d --name cgm-elasticsearch --network egm-suite \
  -p 9200:9200 \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  -e ES_JAVA_OPTS="-Xms1g -Xmx1g" \
  docker.elastic.co/elasticsearch/elasticsearch:8.15.3

docker run -d --name cgm-minio --network egm-suite \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio:RELEASE.2024-12-18T13-15-44Z \
  server /data --console-address ":9001"

docker run -d --name cgm-rabbitmq --network egm-suite \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:4.0-management

docker run -d --name cgm-otel-collector --network egm-suite \
  -p 4318:4318 \
  -v "$PWD/docker/otel-collector.yml:/etc/otel-collector.yml:ro" \
  otel/opentelemetry-collector-contrib:0.114.0 \
  --config=/etc/otel-collector.yml
```

Run the backend service image. The container name `srv-cgm-importer` is used by the GUI nginx proxy.

```bash
docker run -d --name srv-cgm-importer --network egm-suite \
  -p 8080:8080 \
  -e ELASTICSEARCH_URIS=http://cgm-elasticsearch:9200 \
  -e MINIO_ENDPOINT=http://cgm-minio:9000 \
  -e MINIO_ACCESS_KEY=minioadmin \
  -e MINIO_SECRET_KEY=minioadmin \
  -e RABBITMQ_HOST=cgm-rabbitmq \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USER=guest \
  -e RABBITMQ_PASSWORD=guest \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://cgm-otel-collector:4318/v1/traces \
  docker.io/your-dockerhub-user/cgm-srv-cgm-importer:0.1.0-SNAPSHOT
```

Run the GUI image:

```bash
docker run -d --name cgm-gui-explorer --network egm-suite \
  -p 5173:80 \
  docker.io/your-dockerhub-user/cgm-gui-cgm-explorer:0.1.0-SNAPSHOT
```

The GUI container proxies `/api/*` requests to `http://srv-cgm-importer:8080`, so the backend container must be reachable on that Docker network alias or container name.

## API

- `POST /api/cgm/imports`: multipart CGMES import. Required form fields are `file`, `region` (`CORE`, `HANSA`, `IBWT`, `SWE`), and `process` (`CGM`, `CSA`, `CC`, `OPC`, `STA`). Business day, timestamp, time frame, TSO name, CGMES profile type, version, and extension are parsed from file names like `20231016T0030Z_1D_TSCNET-EU-MAVIR_SSH_000.zip`.
- `GET /api/cgm/imports`: list persisted imports for the network id selector, including filename-derived business context, indexed count, and status.
- `GET /api/cgm/networks/{networkId}/equipment`: search/filter indexed equipment. Optional filters include `query`, `type`, `containerId`, `businessDay`, `timestamp`, `region`, `process`, `timeFrame`, `tsoName`, `cgmesProfileType`, `versionNumber`, and `extension`.
- `GET /api/cgm/networks/{leftNetworkId}/compare/{rightNetworkId}`: compare imported network states.
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`.

## Edge Cases

- Large CGMES uploads are capped at 500 MB by default and should move to asynchronous import jobs for multi-GB datasets.
- Zip archives and multi-profile CGMES packages are accepted by the GUI extension filter but need a full PowSyBl importer implementation in `CgmesNetworkReader`.
- Duplicate RDF ids inside one network currently collapse in comparisons by id; production imports should validate and reject duplicate equipment ids.
- Elasticsearch stores each upload as a separate network id; retries of the same source create a new import record and preserve previous indexed data.
- The comparison endpoint compares indexed equipment metadata, not load-flow results yet. Simulation result comparison should become a dedicated `srv.cgm.analysis` module.

## Tests

```bash
mvn test
mvn -pl gui.cgm.explorer package
```
