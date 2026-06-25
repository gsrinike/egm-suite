# data.cnm

`data.cnm` owns DTOs exchanged between CNM GUI, service, and mock modules.

## Package Layout

- `eu.egm.data.cnm.common`: import status, service type, timeframe, profile references, and paged responses.
- `eu.egm.data.cnm.cgmes`: CGMES metadata and profile vocabulary.
- `eu.egm.data.cnm.ncp`: NCP metadata and profile vocabulary.
- `eu.egm.data.cnm.iidm`: lightweight IIDM projections inspired by PowSyBl network concepts.

The DTOs are intentionally storage-neutral and do not depend on Spring MVC, MinIO, Elasticsearch, RabbitMQ, or frontend code.

Import contracts include chunk completion, filename-derived profile types,
separate business day/time fields, searchable profile metadata, and import event
payloads.

Import status uses the aggregate lifecycle states `INIT`, `STORED`, and
`FAILED`. Profile information remains on `ImportFileStatus`, while the parent
`ImportStatus` carries the user-provided import message.

`ImportFileStatus` uses the separate `ImportFileState` lifecycle: `INIT`,
`STORED`, `PARSED`, and `FAILED`. Downstream processing can report file-state
changes without adding file-only states to the parent import.
