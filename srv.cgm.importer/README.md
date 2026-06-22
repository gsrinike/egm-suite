# srv.cgm.importer

## Purpose

`srv.cgm.importer` is the Spring Boot backend service for importing CGMES files, indexing searchable network data, listing imported networks, and comparing imported network states.

The module owns CGMES import and query behavior. Infrastructure access is delegated to `com.infra`, so this service does not directly depend on Elasticsearch, MinIO, or RabbitMQ APIs.

## Main Responsibilities

- Accept multipart CGMES uploads.
- Parse filename metadata from the CGMES naming convention.
- Store raw uploaded files through the utility object-storage abstraction.
- Return after raw files are stored, then process CGMES payloads through the shared `data.cgm` reader on background workers.
- Persist searchable equipment and import-status documents through utility document repositories.
- Publish stored-object and import-completion events through the utility event publisher.
- Expose REST APIs for import, search, import history, and comparison.

## Package Layout

- `api`
  - `CgmImportController`: import and import-history endpoints.
  - `EquipmentController`: equipment search and network comparison endpoints.
- `domain`
  - `EquipmentDocument`: searchable equipment document persisted through the utility document repository.
  - `ImportStatusDocument`: persisted import-history document.
- `repository`
  - `EquipmentSearchRepository`: application adapter around utility document search.
  - `ImportStatusRepository`: application adapter around utility document listing.
- `service`
  - `CgmImportService`: orchestrates import, raw storage, indexing, status persistence, and event publishing.
  - `PowsyblCgmesNetworkReader`: service adapter that delegates CGMES XML/ZIP reading to `data.cgm`.
  - `EquipmentQueryService`: search and comparison use cases.
  - `IidmConversionService`: reads indexed equipment and delegates IIDM DTO projection to `data.cgm`.
  - `RawCgmesStorage`: application wrapper around utility object storage.
  - `CgmesNetworkReader`: reader interface for CGMES parsing.

## REST API

- `GET /api/cgm/imports`
  - Lists imported network ids and business/file context.
- `POST /api/cgm/imports`
  - Multipart import.
  - Required fields: `file`, `region`, `process`.
  - Business day, timestamp, time frame, TSO name, profile type, version, and extension are parsed from filenames.
  - Stores every uploaded file in object storage, saves an `In Progress` import status, publishes `cgm.import.stored`, and returns immediately with the new network id.
- `GET /api/cgm/networks/{networkId}/equipment`
  - Searches indexed equipment.
  - Filters are executed in Elasticsearch through `com.infra`, not in memory.
- `GET /api/cgm/networks/{leftNetworkId}/compare/{rightNetworkId}`
  - Compares indexed equipment metadata between two imports.
- `GET /api/cgm/networks/{networkId}/iidm`
  - Converts indexed equipment rows into an IIDM-oriented DTO projection.

## Filename Convention

Uploads must follow:

```text
<Business Day yyyymmdd><timestamp hhmm>Z_<time_frame>_<TSO Name>_<CGMES Profile Type>_<Version Number>.<extension>
```

The parser also accepts a `T` separator before the timestamp, for example:

```text
20231016T0030Z_1D_TSCNET-EU-MAVIR_SSH_000.zip
```

## Implementation Notes

Search uses a dedicated Elasticsearch query path. The backend no longer fetches the first 10,000 rows and filters in memory, because that could hide valid matches outside the initial batch.

CGMES parsing is intentionally decoupled from service orchestration. Complete CGMES uploads are delegated to the shared data reader in `data.cgm`, which owns the PowSyBl conversion path, graph fallback, and IIDM DTO projection. This service module depends on `data.cgm` for CGM data behavior and does not depend on PowSyBl directly.

Import processing is asynchronous after raw storage. A batch with `n` uploaded files creates `n` internal parsing workers, indexes each file's CGMES equipment projection independently, then updates the import status to `Complete` or `Failed`. The initial screen reads `ImportStatus` documents so users can see upload date, filename context, selected region/process, and current state while background processing continues.

Detailed import, explore, and IIDM invocation sequences are documented in [CGM Import Detail Design](../doc.arch/CGM_IMPORT_DETAIL_DESIGN.md).

Comparison still uses a network-wide read path capped for now. A production-scale comparison should become asynchronous or paged for very large networks.

## Configuration

The service sets `module=srv.cgm.importer` at startup and uses `com.utils.config` to load:

- `base/srv.cgm.importer-application.yml`
- `base/srv.cgm.importer-infra.yml`
- `base/srv.cgm.importer-vault.yml`
- `base/srv.cgm.importer-cache-config.yml`
- `${env}/srv.cgm.importer-application.yml`
- `${env}/srv.cgm.importer-infra.yml`
- `${env}/srv.cgm.importer-vault.yml`
- `${env}/srv.cgm.importer-cache-config.yml`

When `env` or `ENV` is not set, `local` is used.

The base Vault configuration is a sample in `base/srv.cgm.importer-vault.yml`. It defines the client id and allowed secret keys for `${vault:...}` references while leaving `vault.enabled` disabled by default. Environment-specific deployments can add `<env>/srv.cgm.importer-vault.yml` with `vault.enabled: true`, Vault address, and token source.

## Developer Commands

From the repository root:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true -pl srv.cgm.importer -am test
```

Build the backend Docker image through Maven:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.push=true -pl srv.cgm.importer -am package
```

## Runtime Dependencies

The service expects these external services:

- Elasticsearch
- MinIO
- RabbitMQ
- OpenTelemetry collector, when telemetry export is enabled

The Docker Compose file at `../docker/docker-compose.yml` starts the backend with its dependencies.
