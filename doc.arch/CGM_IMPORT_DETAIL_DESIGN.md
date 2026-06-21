# CGM Import Detail Design

This document describes the runtime invocation paths for CGM import, explore, and IIDM projection. The implementation keeps service orchestration in `srv.cgm.importer`, shared CGM data behavior in `data.cgm`, and generic transformer contracts in `com.mapping`.

## Package Boundaries

- `srv.cgm.importer`: REST controllers, service orchestration, storage/index/event adapters.
- `data.cgm.dto.cgmes`: CGMES-facing API and search DTOs.
- `data.cgm.dto.iidm`: IIDM-facing DTOs.
- `data.cgm.mapping`: CGMES filename parsing, PowSyBl-backed reading, fallback graph loading, topology strategies, and IIDM DTO projection.
- `com.mapping`: generic mapping and transformer contracts.
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
    participant Reader as PowsyblCgmesNetworkReader
    participant DataReader as data.cgm.mapping.PowsyblCgmesEquipmentReader
    participant IidmReader as PowsyblIidmEquipmentReader
    participant Fallback as EquipmentProjectionReader
    participant Graph as CgmProfileGraphLoader
    participant Strategy as CgmTopologyMappingStrategy
    participant EquipmentRepo as EquipmentSearchRepository
    participant StatusRepo as ImportStatusRepository
    participant Events as EventPublisherService

    Client->>Controller: POST /api/cgm/imports (multipart files, region, process)
    Controller->>Service: importCgmes(files, region, process)
    Service->>Parser: parse(originalFilename, region, process)
    Parser-->>Service: ImportMetadata
    Service->>Storage: store(networkId/fileName, bytes, contentType)
    Service->>Reader: read(networkId, metadata, InputStream)
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
    Reader-->>Service: indexed equipment projections
    Service->>EquipmentRepo: saveAll(EquipmentDocument.fromView)
    Service->>StatusRepo: save(ImportStatusDocument.fromStatus)
    Service->>Events: publish(imported status)
    Service-->>Controller: ImportStatus
    Controller-->>Client: ImportStatus
```

## 2. Explore Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Controller as EquipmentController
    participant Service as EquipmentQueryService
    participant Repo as EquipmentSearchRepository
    participant Infra as com.infra DocumentRepositoryService

    Client->>Controller: GET /api/cgm/networks/{networkId}/equipment
    Controller->>Service: search(networkId, SearchRequest)
    Service->>Repo: search(networkId, SearchRequest)
    Repo->>Infra: search(index, filters, page, sort)
    Infra-->>Repo: DocumentPage<EquipmentDocument>
    Repo-->>Service: DocumentPage<EquipmentDocument>
    Service->>Service: map EquipmentDocument to EquipmentView
    Service-->>Controller: PageResponse<EquipmentView>
    Controller-->>Client: PageResponse<EquipmentView>

    Client->>Controller: GET /api/cgm/networks/{left}/compare/{right}
    Controller->>Service: compare(leftNetworkId, rightNetworkId)
    Service->>Repo: findByNetworkId(leftNetworkId)
    Repo-->>Service: left equipment documents
    Service->>Repo: findByNetworkId(rightNetworkId)
    Repo-->>Service: right equipment documents
    Service->>Service: calculate added, removed, changed equipment
    Service-->>Controller: NetworkDiff
    Controller-->>Client: NetworkDiff
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
