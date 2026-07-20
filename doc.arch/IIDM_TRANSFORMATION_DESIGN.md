# IIDM Transformation Design

## Purpose

IIDM transformation starts after CNM RDF metadata extraction has produced a typed
`ProfilePayload` JSON document. The transformation does not parse raw RDF/XML and
maps persisted CNM profile DTOs into PowSyBl IIDM `Network` objects. The
PowSyBl dependency is intentionally owned by `data.iidm` and `map.cnm.iidm` so
load-flow, security-analysis, and RAO services can consume real IIDM networks
without reinterpreting CGMES payloads.

## Module Ownership

- `data.iidm`: owns PowSyBl IIDM network wrappers, XIIDM helpers, transform
  state, and transform event contracts. It is the explicit module where
  `com.powsybl.*` dependencies are allowed.
- `map.cnm.iidm`: owns CGMES/NCP profile DTO to PowSyBl IIDM `Network` mapping.
  It depends on `data.cnm`, `data.iidm`, and `com.mapping`.
- `srv.iidm.transformer`: owns RabbitMQ event consumption, CNM profile payload
  reads, IIDM transform status persistence, IIDM network persistence, and IIDM
  REST exploration APIs.
- `srv.cnm.services`: owns CNM import and RDF metadata extraction only. After a
  file is parsed, it publishes an IIDM transform request event.
- `com.infra`: owns document storage and messaging adapters.
- `com.mapping`: owns DTO-to-JSON and JSON-to-DTO conversion contracts.

`srv.cnm.services` must not depend on `map.cnm.iidm` or `srv.iidm.transformer`.
The hand-off between CNM and IIDM is an event using `data.iidm` contracts. GUI
modules and `data.cnm` remain free of PowSyBl dependencies.

The CNM service declares the IIDM event exchange as an additional messaging
exchange because it publishes `IidmProfileTransformRequested` before or
independently of the IIDM transformer consumer startup.

## Document Ownership

CNM document indices:

- `cnm-imports`: import aggregate and file processing status.
- `cnm-profiles`: searchable profile metadata only.
- `cnm-profile-payloads`: large profile DTO JSON payloads keyed by `fileId`.

IIDM document indices:

- `iidm-profile-transforms`: one transform status document per processed profile
  file. It records source payload ID, transform state, diagnostics, and the
  resulting IIDM network ID.
- `iidm-networks`: PowSyBl IIDM networks exported in XIIDM format and keyed by
  network ID. Searchable fields contain import ID, source file IDs, TSO,
  timeframe, business day, business time, and stable element counts.

Large payload fields remain XIIDM text or chunks. Search and list APIs should
query stable metadata fields and should not read the XIIDM payload unless the
caller requests a concrete network.

## GUI Visualization

The `IIDM` menu in `gui.cnm.manager` is profile-scoped and lazy-loaded:

- The initial view first requires selecting a successful CNM import. The import
  selector contains only imports whose aggregate state is `SUCCESS`.
- After selection, the view calls the transform list API with `importId` and
  receives transform/network metadata only. It does not receive `networkXiidm`
  or `networkXiidmChunks`.
- Rows are clickable only when the transform state is `DONE` and a network ID is
  available.
- Selecting one transformed profile opens an IIDM detail view for that network.
- The first detail request returns table metadata only: table IDs, labels,
  columns, and total row counts.
- Rows are fetched with a second request for the selected table and page.
- `srv.iidm.transformer` reads the stored XIIDM network only for the selected
  table request, and converts only that table family into rows.

This keeps large IIDM networks out of browser memory and avoids expanding all
network objects merely to show the list or table selector.

## Event Flow

```mermaid
sequenceDiagram
    autonumber
    participant CNM as "srv.cnm.services"
    participant CnmPayloads as "cnm-profile-payloads"
    participant Events as "RabbitMQ iidm.events"
    participant IIDM as "srv.iidm.transformer"
    participant Mapping as "map.cnm.iidm"
    participant TransformIndex as "iidm-profile-transforms"
    participant NetworkIndex as "iidm-networks"

    CNM->>CnmPayloads: save ProfilePayload JSON after RDF metadata extraction
    CNM->>Events: publish IidmProfileTransformRequested
    Events-->>IIDM: consume transform request
    IIDM->>TransformIndex: upsert STARTED transform document
    IIDM->>CnmPayloads: read ProfilePayload JSON by fileId
    IIDM->>IIDM: deserialize JSON through com.mapping
    IIDM->>Mapping: transform ProfilePayload to PowSyBl Network
    Mapping-->>IIDM: IidmNetworkModel with PowSyBl Network and diagnostics
    IIDM->>IIDM: export Network as XIIDM
    IIDM->>NetworkIndex: save XIIDM payload and stable counts
    IIDM->>TransformIndex: mark DONE with network ID
    IIDM->>Events: publish IidmProfileTransformCompleted

    alt transformation fails
        IIDM->>TransformIndex: mark FAILED with diagnostic message
        IIDM->>Events: publish IidmProfileTransformFailed
    end
```

## Mapping Rules

The first implementation uses a PowSyBl bus/breaker network projection:

- common topology objects become PowSyBl substations, voltage levels, and buses.
- line, load, and generator objects become PowSyBl connectables when terminal
  and bus references are resolvable.
- SV voltage values are applied to PowSyBl buses where topology references are
  resolvable.
- unresolved relations are retained as diagnostics instead of failing the whole
  profile transform.

Future increments should extend this from profile-level networks to grouped
CGMES model assembly. EQ should establish the base network, SSH/SV/TP should
update the same PowSyBl network variant or a derived variant, following
PowSyBl's `Network.read(...)` and `network.update(...)` style for profile
updates where direct CGMES source files are available.

## REST Surface

`srv.iidm.transformer` exposes:

- `GET /api/iidm/transforms?importId={importId}&search={search}&page={page}&size={size}`:
  list lightweight profile transform status rows for IIDM menu screens. Search
  is applied against metadata fields such as file ID, profile, state, message,
  and network ID.
- `GET /api/iidm/transforms/{fileId}`: read one transform status by source file.
- `GET /api/iidm/networks?importId={importId}&page={page}&size={size}`: list
  lightweight IIDM network metadata for an import. The response excludes XIIDM
  payload fields.
- `GET /api/iidm/networks/{networkId}`: read one persisted IIDM network document.
- `GET /api/iidm/imports/{importId}/files/{fileId}/tables`: load table metadata
  for the selected transformed profile/file.
- `GET /api/iidm/networks/{networkId}/tables`: load table metadata for one
  selected transformed network.
- `GET /api/iidm/networks/{networkId}/tables/{tableId}?page={page}&size={size}&search={search}`:
  load one page of rows for the selected IIDM table.

The full network document endpoint is retained for service diagnostics. GUI code
uses the metadata and paged table endpoints. IIDM dynamic-table search is
server-side so a term can match rows beyond the currently displayed page.

## Consistency Rules

- CNM marks a file `PARSED` only after its profile metadata and profile payload
  are durable.
- IIDM transform status starts at `STARTED` when an event is consumed.
- IIDM transform status moves to `DONE` only after the IIDM network document is
  saved.
- IIDM transform status moves to `FAILED` if source payload retrieval, JSON
  mapping, PowSyBl network creation, XIIDM export, or network persistence fails.
- Transform event handling is idempotent by `fileId`: reprocessing replaces the
  transform document and network document for the same source file.
