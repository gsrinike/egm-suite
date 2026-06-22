# CGM Import Detail Design

This document describes the runtime invocation paths for CGM import, explore, and IIDM projection. The implementation keeps service orchestration in `srv.cgm.importer`, shared CGM data behavior in `data.cgm`, and generic transformer contracts in `com.mapping`.

## Package Boundaries

- `srv.cgm.importer`: REST controllers, service orchestration, storage/index/event adapters.
- `data.cgm.dto.cgmes`: CGMES-facing API and search DTOs.
- `data.cgm.dto.iidm`: IIDM-facing DTOs.
- `data.cgm.mapping`: CGMES filename parsing, PowSyBl-backed reading, fallback graph loading, topology strategies, and IIDM DTO projection.
- `com.mapping`: generic mapping and transformer contracts.
- `com.infra.storage.document`: generic document repository contracts consumed by import-status and equipment repositories.
- `com.infra.storage.object`: generic object-storage contract used by raw CGMES upload storage.
- `com.infra.event`: generic event-publishing contract used for import lifecycle messages.
- `map.cgm`: reusable CGMES/IIDM transformer implementation using `com.mapping`.

## 1. Import Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Controller as CgmImportController
    participant Service as CgmImportService
    participant Parser as CgmesFileNameParser
    participant Storage as RawCgmesStorage
    participant Minio as MinIO Object Store
    participant Worker as Per-file Import Workers
    participant Reader as PowsyblCgmesNetworkReader
    participant DataReader as data.cgm.mapping.PowsyblCgmesEquipmentReader
    participant IidmReader as PowsyblIidmEquipmentReader
    participant Fallback as EquipmentProjectionReader
    participant Graph as CgmProfileGraphLoader
    participant Strategy as CgmTopologyMappingStrategy
    participant EquipmentRepo as EquipmentSearchRepository
    participant StatusRepo as ImportStatusRepository
    participant Events as EventPublisherService
    participant Rabbit as RabbitMQ

    Client->>Controller: POST /api/cgm/imports (multipart files, region, process)
    Controller->>Service: importCgmes(files, region, process)
    loop Each uploaded file
        Service->>Parser: parse(originalFilename, region, process)
        Parser-->>Service: ImportMetadata
        Service->>Storage: store(networkId/sequence-fileName, bytes, contentType)
        Storage->>Minio: putObject(raw CGMES bytes)
        Minio-->>Storage: object id stored
    end
    Service->>StatusRepo: save(ImportStatus state=In Progress)
    Service->>Events: publish(cgm.import.stored, networkId, objectIds)
    Events->>Rabbit: stored-object event
    Service-->>Controller: ImportStatus state=In Progress
    Controller-->>Client: ImportStatus state=In Progress

    Service->>Worker: spawn one worker per stored file
    par Worker per uploaded file
        Worker->>Reader: read(networkId, metadata, InputStream)
        Reader->>DataReader: read(networkId, metadata, InputStream)
        DataReader->>DataReader: copy payload bytes
        DataReader->>IidmReader: read(PowSyBl imported Network)
        alt PowSyBl conversion succeeds
            IidmReader-->>DataReader: List<IidmEquipment>
            DataReader-->>Reader: List<EquipmentView>
        else Conversion fails or yields no equipment
            DataReader->>Fallback: read(networkId, metadata, payload)
            Fallback->>Graph: load(payload)
            Graph-->>Fallback: CgmProfileGraph
            Fallback->>Strategy: supports(graph, metadata)
            Strategy-->>Fallback: selected topology strategy
            Fallback->>Strategy: project(networkId, metadata, graph)
            Strategy->>Strategy: pass 1 instantiate equipment/state by mRID
            Strategy->>Strategy: pass 2 resolve containers/topology associations
            Strategy-->>Fallback: List<EquipmentView>
            Fallback-->>DataReader: List<EquipmentView>
            DataReader-->>Reader: List<EquipmentView>
        end
        Reader-->>Worker: indexed equipment projections
        Worker->>EquipmentRepo: saveAll(EquipmentDocument.fromView)
    end
    Worker-->>Service: all workers completed
    Service->>StatusRepo: save(ImportStatus state=Complete or Failed)
    Service->>Events: publish(cgm.import.completed, final status)
    Events->>Rabbit: completion event
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
