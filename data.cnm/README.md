# data.cnm

`data.cnm` owns DTOs exchanged between CNM GUI, service, and mock modules.

## Package Layout

- `eu.egm.data.cnm.common`: import status, service type, timeframe, profile references, and paged responses.
- `eu.egm.data.cnm.cgmes`: CGMES metadata, profile vocabulary, and profile-specific DTOs.
- `eu.egm.data.cnm.nc`: Network Code profile metadata and profile vocabulary.

`ProfileFamily` deliberately contains only top-level families: `CGMES`, `NCP`,
and `Unknown`. Profile-specific values such as `EQ`, `SSH`, `SV`, `TP`, `DL`,
`GL`, `AEAS`, `SAR`, and `SSI` live in `CgmesProfileKind` and `NCProfileKind`.

IIDM projections and IIDM transform events live in the separate `data.iidm`
module so CNM profile DTOs stay focused on RDF metadata and common topology.

The DTOs are intentionally storage-neutral and do not depend on Spring MVC, MinIO, Elasticsearch, RabbitMQ, or frontend code.

Import contracts include chunk completion, filename-derived profile types,
separate business day/time fields, searchable profile metadata, and import event
payloads.

Import status uses the aggregate lifecycle states `INIT`, `STORED`, `SUCCESS`,
and `FAILED`. `STORED` means durable intake has completed and file-processing
events were queued. `SUCCESS` means every file has been parsed successfully.
Profile information remains on `ImportFileStatus`, while the parent
`ImportStatus` carries the user-provided import message.

`ImportFileStatus` uses the separate `ImportFileState` lifecycle: `INIT`,
`STORED`, `PARSED`, and `FAILED`. File-processing events move files from
`STORED` to `PARSED` or `FAILED` without adding file-only states to the parent
import.
