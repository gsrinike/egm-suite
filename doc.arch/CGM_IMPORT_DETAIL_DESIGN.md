# CGM Import Detail Design

This document describes the runtime invocation paths for CGM import, explore, and IIDM projection. The implementation keeps service orchestration in `srv.cgm.importer`, shared CGM data behavior in `data.cgm`, and generic transformer contracts in `com.mapping`.

## Package Boundaries

- `srv.cgm.importer`: REST controllers, service orchestration, storage/index/event adapters.
- `bpm.cgm.import`: embedded Camunda BPMN process and delegates for process id `cgm-import`.
- `data.cgm.dto.cgmes`: CGMES-facing API and search DTOs.
- `data.cgm.dto.iidm`: IIDM-facing DTOs.
- `data.cgm.mapping`: CGMES filename parsing, PowSyBl-backed reading, fallback graph loading, topology strategies, and IIDM DTO projection.
- `com.mapping`: generic mapping and transformer contracts.
- `com.infra.storage.document`: generic document repository contracts consumed by import-status and equipment repositories.
- `com.infra.storage.object`: generic object-storage contract used by raw CGMES upload storage.
- `com.infra.event`: generic event-publishing contract used for import lifecycle messages.
- `com.infra.bpm`: generic BPM process contract used to start and monitor Camunda process instances.
- `map.cgm`: reusable CGMES/IIDM transformer implementation using `com.mapping`.

## 1. Import Flow

```mermaid
sequenceDiagram
    autonumber
    actor GUI
    participant ImportController as srv.cgm.importer CgmImportController
    participant ImportService as CgmImportService
    participant Storage as RawCgmesStorage
    participant Minio as MinIO Object Store
    participant StatusRepo as ImportStatusRepository
    participant InfraBpm as com.infra BusinessProcessService
    participant Bpm as bpm.cgm.import cgm-import BPMN
    participant Transform as CgmesTransform API
    participant Reader as PowsyblCgmesNetworkReader
    participant DataReader as data.cgm mapping readers
    participant EquipmentRepo as EquipmentSearchRepository
    participant Events as EventPublisherService
    participant Rabbit as RabbitMQ
    participant IidmListener as CgmesTransformedDocumentsListener
    participant IidmService as IidmConversionService

    GUI->>ImportController: POST /api/cgm/imports (multipart files, region, process)
    ImportController->>ImportService: importCgmes(files, region, process)
    ImportService->>ImportService: create networkId
    par One storage worker per uploaded file
        ImportService->>Storage: store(objectId, bytes, contentType)
        Storage->>Minio: putObject(raw CGMES bytes)
        Minio-->>Storage: stored
    end
    ImportService->>StatusRepo: save ImportStatus(state=Init, file statuses=Init)
    ImportService->>Events: publish cgm.import.stored(networkId, objectIds)
    Events->>Rabbit: stored-object event
    ImportService-->>ImportController: ImportStatus state=Init
    ImportController-->>GUI: ImportStatus state=Init

    GUI->>ImportController: POST /api/cgm/imports/processes/start ImportStatus
    ImportController->>ImportService: startCgmImport(status)
    ImportService->>InfraBpm: start(processId=cgm-import, variables)
    InfraBpm->>Bpm: start embedded Camunda process
    Bpm->>Bpm: Init task extracts objectIds from ImportStatus
    ImportService->>StatusRepo: save ImportStatus(state=In Progress, processInstanceId)
    ImportController-->>GUI: ImportStatus with processInstanceId

    par BPM multi-instance over objectIds
        Bpm->>Transform: POST /api/cgm/imports/{networkId}/transforms/cgmes(objectId)
        Transform->>ImportService: transformObject(networkId, objectId)
        ImportService->>StatusRepo: update file status=Started, iidmTransformStatus=Started
        ImportService->>Storage: read(objectId)
        Storage->>Minio: getObject(raw CGMES bytes)
        Minio-->>Storage: bytes
        ImportService->>Reader: read(networkId, metadata, InputStream)
        Reader->>DataReader: PowSyBl conversion or fallback graph projection
        DataReader-->>Reader: List<EquipmentView>
        Reader-->>ImportService: equipment projections
        ImportService->>EquipmentRepo: saveAll(EquipmentDocument.fromView)
        ImportService->>Events: publish cgm.cgmes.transformed(documentIds)
        Events->>Rabbit: transformed-document event
        ImportService->>StatusRepo: update file status=Complete or Failed
    end

    Rabbit->>IidmListener: cgm.cgmes.transformed
    IidmListener->>IidmService: convert(networkId)
    IidmService-->>IidmListener: IidmNetwork projection
    IidmListener->>StatusRepo: update iidmTransformStatus=Done or Failed
    GUI->>ImportController: GET /api/cgm/imports/{networkId}/process-history
    ImportController-->>GUI: process instance id and per-file statuses
```

## 2. Explore Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant ImportController as CgmImportController
    participant ImportService as CgmImportService
    participant StatusRepo as ImportStatusRepository
    participant EquipmentController as EquipmentController
    participant QueryService as EquipmentQueryService
    participant EquipmentRepo as EquipmentSearchRepository
    participant Infra as com.infra.storage.document.DocumentRepositoryService

    Client->>ImportController: GET /api/cgm/imports
    ImportController->>ImportService: listImports()
    ImportService->>StatusRepo: findAll()
    StatusRepo-->>ImportService: upload dates, file context, state
    ImportService-->>ImportController: List<ImportStatus>
    ImportController-->>Client: Initial import status screen

    Client->>EquipmentController: GET /api/cgm/networks/{networkId}/equipment
    EquipmentController->>QueryService: search(networkId, SearchRequest)
    QueryService->>EquipmentRepo: search(networkId, SearchRequest)
    EquipmentRepo->>Infra: search(index, filters, page, sort)
    Infra-->>EquipmentRepo: DocumentPage<EquipmentDocument>
    EquipmentRepo-->>QueryService: DocumentPage<EquipmentDocument>
    QueryService->>QueryService: map EquipmentDocument to EquipmentView
    QueryService-->>EquipmentController: PageResponse<EquipmentView>
    EquipmentController-->>Client: PageResponse<EquipmentView>

    Client->>EquipmentController: GET /api/cgm/networks/{left}/compare/{right}
    EquipmentController->>QueryService: compare(leftNetworkId, rightNetworkId)
    QueryService->>EquipmentRepo: findByNetworkId(leftNetworkId)
    EquipmentRepo-->>QueryService: left equipment documents
    QueryService->>EquipmentRepo: findByNetworkId(rightNetworkId)
    EquipmentRepo-->>QueryService: right equipment documents
    QueryService->>QueryService: calculate added, removed, changed equipment
    QueryService-->>EquipmentController: NetworkDiff
    EquipmentController-->>Client: NetworkDiff
```

## 3. IIDM Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Controller as IidmConversionController
    participant Service as IidmConversionService
    participant Repo as EquipmentSearchRepository
    participant Mapper as data.cgm.mapping.CgmIidmNetworkMapper

    Client->>Controller: GET /api/cgm/networks/{networkId}/iidm
    Controller->>Service: convert(networkId)
    Service->>Repo: findByNetworkId(networkId)
    Repo-->>Service: List<EquipmentDocument>
    Service->>Service: map documents to EquipmentView
    Service->>Mapper: mapNetwork(networkId, equipmentViews)
    Mapper->>Mapper: map EquipmentType to IidmEquipmentType
    Mapper->>Mapper: group equipment into IidmNetwork
    Mapper-->>Service: IidmNetwork
    Service-->>Controller: IidmNetwork
    Controller-->>Client: IidmNetwork
```

## Design Notes

- `srv.cgm.importer` orchestrates workflows and persistence but does not import PowSyBl APIs.
- `data.cgm` owns the CGMES reading details and contains all PowSyBl CGMES/IIDM dependencies.
- Fallback RDF/XML projection uses an interim `CgmProfileGraph` and a two-pass strategy pipeline instead of direct raw XML-to-IIDM mapping.
- Node-breaker and bus-branch behavior is selected through `CgmTopologyMappingStrategy`, allowing future topology variants without changing service orchestration.
