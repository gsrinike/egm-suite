# CNM Import Design

## Purpose

The Common Network Model (CNM) import application is the first application surface for RDF-based grid model intake. It supports Common Grid Model (CGM), Coordinated Security Analysis (CSA), and Capacity Calculation (CC) use cases across intra-day, day-ahead, and day-two timeframes.

The initial implementation focuses on import orchestration, metadata capture, and reusable module boundaries. Semantic graph validation, PowSyBl-backed IIDM transformation, CSA, and CC calculation flows are expected to build on this foundation.

## Profile Sources

The application accepts RDF profile files aligned with the ENTSO-E application profile library. The profile repository organizes CGMES and NCP profile definitions into dedicated folders and publishes RDFS, SHACL, and profile metadata packages. Import code should treat these profile definitions as external contracts, not as hand-written business assumptions.

## Modules

- `data.cnm`: transport DTOs shared by GUI, service, and mock modules. Packages are separated into `common`, `cgmes`, `ncp`, and `iidm`.
- `srv.cnm.services`: Spring Boot REST service exposing CNM import APIs and OpenAPI documentation.
- `mock.srv.cnm.services`: Spring Boot mock service with in-memory import data for GUI development.
- `gui.common`: Vue shared components for buttons, links, menus, dropdowns, and searchable/sortable/paginated tables.
- `gui.cnm.manager`: Vue application for RDF upload and import status visualization.

## Import Scope

The first import flow does not map raw XML/RDF directly into IIDM objects. It extracts RDF metadata, classifies profile references as CGMES or NCP where possible, stores the raw payload in object storage through `com.infra`, and persists import metadata in the document store through `com.infra`.

Later semantic import stages should load RDF into a graph or intermediate model before mapping. Complex relationship resolution should use a two-pass pipeline:

1. Instantiate core objects and store them by mRID.
2. Resolve topology and associations using lookup maps.

Strategy-based mapping should be used when topology or profile metadata indicates different mapping behavior, such as bus-branch and node-breaker variants.

## REST Surface

The production service owns the OpenAPI contract under `srv.cnm.services/src/main/resources/openapi/cnm-services.yaml`.

Initial endpoints:

- `POST /api/cnm/imports`: accepts service type, timeframe, optional message, and RDF profile file.
- `POST /api/cnm/imports/failures`: records an upload rejected by a proxy, network, or multipart boundary.
- `GET /api/cnm/imports`: returns import status rows with optional free-text filtering.
- `GET /api/cnm/imports/{importId}`: returns a single import status.
- `PUT /api/cnm/imports/{importId}/files/{fileId}/status`: accepts downstream file-state updates.

The mock service follows the same route shape for GUI development.

## Sequence

```mermaid
sequenceDiagram
    autonumber
    participant User as "User"
    participant GUI as "gui.cnm.manager"
    participant Boot as "Spring application startup"
    participant API as "CnmImportController"
    participant Chunks as "CnmChunkUploadService"
    participant Import as "CnmImportRestService"
    participant RDF as "RdfMetadataExtractor"
    participant Infra as "com.infra InfrastructureUtils"
    participant MinIO as "MinIO ObjectStorageService"
    participant Imports as "Elasticsearch cnm-imports"
    participant Profiles as "Elasticsearch cnm-profiles"
    participant Events as "RabbitMQ EventPublisherService"
    participant Downstream as "Downstream processor"

    rect rgb(245, 247, 250)
        Note over Boot,MinIO: Application initialization
        Boot->>Infra: create configured infrastructure capabilities
        Infra->>MinIO: initializeBucket(cnm-rdf-models)
        MinIO-->>Infra: bucket ready
        Infra-->>Boot: service initialization continues
    end

    User->>GUI: select service, timeframe, message, and one or more RDF/ZIP files
    GUI->>GUI: create import ID

    loop each source file
        GUI->>GUI: reject files larger than 1 GB
        GUI->>GUI: split file into 8 MB chunks
        loop each chunk
            GUI->>API: POST /api/cnm/imports/chunks with upload coordinates
            API->>Chunks: storeChunk(importId, fileId, index, bytes)
            Chunks->>Chunks: validate IDs, size, and chunk coordinates
            Chunks->>Chunks: persist part in temporary staging directory
            API-->>GUI: 200 chunk accepted
        end
    end

    GUI->>API: POST /api/cnm/imports/chunks/complete with message
    API->>Chunks: complete(importId)
    Chunks->>Chunks: assemble parts in index order
    Chunks->>Chunks: validate every part and final file size
    Chunks-->>API: staged MultipartFile list
    API->>Import: importModels(files, serviceType, timeframe, importId, message)
    Import->>Imports: save aggregate INIT document and INIT file rows

    loop each staged source
        Import->>Import: recursively expand ZIP and nested ZIP entries
        Import->>Import: ignore metadata entries and collect RDF/XML payloads
    end

    alt no RDF/XML payload was found or extraction failed
        Import->>Imports: replace import with FAILED document
        Import-->>API: FAILED ImportStatus
    else RDF/XML payloads were found
        Import->>Import: create one worker thread per RDF/XML payload
        par each RDF/XML payload
            Import->>Import: parse filename metadata
            Note right of Import: timestamp, timeframe, TSO,<br/>profile type, version, and profile family
            Import->>MinIO: store(cnm-rdf-models, importId/path, bytes)
            alt object storage and metadata extraction succeed
                MinIO-->>Import: object stored
                Import->>RDF: extract RDF profile references
                RDF-->>Import: CGMES/NCP metadata
                Import->>Import: create PARSED file document
            else processing fails
                Import->>Import: capture exception as FAILED file document
            end
        end

        Import->>Import: aggregate state as STORED or FAILED
        Import->>Imports: replace aggregate import document
        Import->>Profiles: save one searchable profile document per PARSED file
        Import->>Events: publish cnm.import.completed with profile metadata
        Import-->>API: ImportStatus
    end

    API->>Chunks: discard(importId) in finally block
    API-->>GUI: completed ImportStatus
    GUI->>GUI: show aggregate INIT, STORED, or FAILED state

    opt view files for an import
        User->>GUI: select the File link
        GUI->>API: GET /api/cnm/imports/{importId}
        API->>Import: findImport(importId)
        Import->>Imports: findByField(id, importId)
        Imports-->>Import: persisted CnmImportDocument
        Import->>Import: restore missing filename metadata
        Import->>Import: normalize epoch, numeric-string, ISO, or legacy Instant timestamps
        Import-->>API: ImportStatus with file details
        API-->>GUI: import and file-level metadata
        GUI->>GUI: show searchable, sortable, paginated file table
    end

    opt search imported profiles
        User->>GUI: filter by profile, TSO, business day, or business time
        GUI->>API: GET /api/cnm/imports/profiles
        API->>Import: searchProfiles(filters, page, size)
        Import->>Profiles: filtered document search
        Profiles-->>Import: matching profile documents
        Import-->>API: paginated profile metadata
        API-->>GUI: profile search results
    end

    opt downstream file processing
        Downstream->>API: PUT file status as STORED, PARSED, or FAILED
        API->>Import: updateFileStatus(importId, fileId, state)
        Import->>Imports: replace file status and recompute aggregate state
        Import->>Profiles: update matching profile state
        Import-->>API: updated ImportStatus
        API-->>Downstream: updated ImportStatus
    end

    opt chunk, proxy, network, or completion failure
        GUI->>GUI: retain import ID and expose re-upload
        GUI->>API: POST /api/cnm/imports/failures
        API->>Import: reportFailure(importId, file names, message)
        Import->>Imports: save FAILED import and file rows
        API-->>GUI: failed ImportStatus
        User->>GUI: select Re-upload
        GUI->>API: repeat chunk upload with the same import ID
    end
```

## Large Uploads And Retry

The GUI splits each source file into 8 MB binary chunks and supports a logical
file size of up to 1 GB. Nginx and Spring use a 16 MB per-request limit. The
service stages chunks on disk, validates completeness and size, then starts the
existing ZIP/RDF import pipeline.

The GUI creates the import ID before sending the multipart request. If a proxy or network error prevents the multipart request from reaching the service, the GUI sends a small failure report so the import still appears with `FAILED` status. Re-upload replaces that document under the same import ID, first with `INIT` and then with the completed or failed per-file state.

The aggregate import table intentionally omits profile columns because one
source bundle can contain multiple profiles. Its File column links to a
dedicated detail view using the same shared table behavior. Successful imports
finish as `STORED`; the only aggregate states are `INIT`, `STORED`, and
`FAILED`. The optional message entered beside the RDF model selector is stored
on `ImportStatus`.

File lifecycle is intentionally separate. `ImportFileState` contains `INIT`,
`STORED`, `PARSED`, and `FAILED`. A parsed file does not add `PARSED` to the
aggregate lifecycle; a set of stored or parsed files yields aggregate
`STORED`, while any failed file yields aggregate `FAILED`. Downstream event
handlers or services use the file-status callback to persist their progress.

The filename pattern
`<Timestamp>_<Time Frame>_<TSO Name>_<Profile Type>_<Version>` populates both
the import-file document and profile document. The literal profile code,
derived profile family, and file state are separate fields. New persisted
timestamps are epoch milliseconds. Document fields are read schema-tolerantly,
then numeric, numeric-string, ISO, and legacy `Instant` values are normalized
to API `Instant` values when responses are assembled. Missing business day and
business time values are reconstructed from the filename.

Filename metadata is authoritative for profile type, TSO, timeframe, version,
business day, and business time. For example,
`20241202T2330Z_1D_TSCNET-EU_SV_002.xml` is stored as profile `SV`, business day
`2024-12-02`, and business time `23:30`.

After successful object storage, the service stores profile metadata in the
`cnm-profiles` Elasticsearch index and publishes a `cnm.import.completed` event
through `com.infra`. The profile search API filters by profile type, TSO,
business day, and business time.

## Dependency Rules

- `srv.cnm.services` invokes object and document storage only through `com.infra`.
- `srv.cnm.services` consumes `data.cnm` DTOs and does not depend on GUI or mock modules.
- `mock.srv.cnm.services` consumes `data.cnm` and `com.utils`, but not production infrastructure.
- `gui.cnm.manager` consumes `gui.common` and calls REST APIs over HTTP.
- `data.cnm` remains independent of Spring, PowSyBl, Elasticsearch, MinIO, RabbitMQ, and Vue.

## Local Deployment

The local Docker Compose stack can run infrastructure, the CNM service, the mock service, and the Vue manager from locally built artifacts.

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true clean package
docker compose -f docker/docker-compose.yml up
```

The production CNM service expects Elasticsearch, MinIO, RabbitMQ, and OpenTelemetry endpoint configuration through environment-specific YAML and container environment variables. The mock service can run without infrastructure.
