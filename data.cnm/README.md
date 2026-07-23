# data.cnm

`data.cnm` owns DTOs exchanged between CNM GUI, service, and mock modules.

## Package Layout

- `eu.egm.data.cnm.common`: import status, service type, timeframe, profile references, and paged responses.
- `eu.egm.data.cnm.cgmes`: CGMES metadata, profile vocabulary, and profile-specific DTOs.
- `eu.egm.data.cnm.nc`: Network Code profile metadata and profile vocabulary.
- `eu.egm.data.cnm.rdf`: neutral RDF profile facts, profile fragments, and mRID index contracts.
- `eu.egm.data.cnm.topology`: assembled static topology model wrappers.
- `eu.egm.data.cnm.state`: dynamic SSH/SV operating-state snapshot DTOs.
- `eu.egm.data.cnm.snapshot`: stitched CGM snapshot and incremental update contracts.

`ProfileFamily` deliberately contains only top-level families: `CGMES`, `NCP`,
and `Unknown`. Profile-specific values such as `EQ`, `SSH`, `SV`, `TP`, `DL`,
`GL`, `AEAS`, `SAR`, and `SSI` live in `CgmesProfileKind` and `NCProfileKind`.

IIDM projections and IIDM transform events live in the separate `data.iidm`
module so CNM profile DTOs stay focused on RDF metadata and common topology.

The DTOs are intentionally storage-neutral and do not depend on Spring MVC, MinIO, Elasticsearch, RabbitMQ, or frontend code.

Import contracts include chunk completion, filename-derived profile types,
separate business day/time fields, searchable profile metadata, and import event
payloads. `CnmFileProcessingRequested` is used for RDF/profile extraction;
`CnmSnapshotAssemblyRequested` is used for the heavier stitched snapshot build
after every file in a model group is parsed.

`ProfileFragment` captures one parsed RDF/XML or `.idm` file without binding it
to storage or PowSyBl. `CgmNetworkSnapshot` stitches related profile fragments
for a TSO/business day/business time/timeframe into static topology plus dynamic
state so downstream IIDM conversion can work on a complete model view.
`CnmSnapshotMetadata` describes the stored snapshot without embedding large
payloads; large snapshot sections remain an implementation detail of the
service-owned document store.

Import status uses the aggregate lifecycle states `INIT`, `STORED`, `SUCCESS`,
and `FAILED`. `STORED` means durable intake has completed and file-processing
events were queued. `SUCCESS` means every file has been parsed successfully.
Profile information remains on `ImportFileStatus`, while the parent
`ImportStatus` carries the user-provided import message.

`ImportFileStatus` uses the separate `ImportFileState` lifecycle: `INIT`,
`STORED`, `PARSED`, and `FAILED`. File-processing events move files from
`STORED` to `PARSED` or `FAILED` without adding file-only states to the parent
import.
