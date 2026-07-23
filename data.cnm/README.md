# data.cnm

`data.cnm` owns transport DTOs for Common Network Model import and exploration.
It is storage-neutral and does not depend on Spring, Elasticsearch, MinIO,
RabbitMQ, or frontend modules.

## Packages

- `eu.egm.data.cnm.common`: import states, file states, service type, timeframe,
  profile metadata, events, paged responses, and dynamic table contracts.
- `eu.egm.data.cnm.cgmes`: CGMES profile vocabulary and profile-specific DTOs.
- `eu.egm.data.cnm.nc`: Network Code profile vocabulary and DTOs.
- `eu.egm.data.cnm.rdf`: RDF fact, fragment, and mRID index DTOs.
- `eu.egm.data.cnm.topology`: reusable static topology model DTOs.
- `eu.egm.data.cnm.state`: SSH/SV state DTOs.
- `eu.egm.data.cnm.snapshot`: stitched CGM snapshot and update DTOs.
- `eu.egm.data.cnm.iidm`: lightweight IIDM summary DTOs used by CNM surfaces.

`ProfileFamily` contains only `CGMES`, `NCP`, and `Unknown`. Concrete profile
types are represented by `CgmesProfileKind` and `NCProfileKind`.

## Current Contracts

- Uploads use aggregate `ImportState` and per-file `ImportFileState`.
- RDF processing uses `CnmFileProcessingRequested`.
- Snapshot assembly uses `CnmSnapshotAssemblyRequested`.
- Profile payloads use typed DTOs plus common topology objects.
- Dynamic profile/IIDM tables use `DynamicTableBundle`,
  `DynamicTableDefinition`, `DynamicTableColumn`, and `DynamicTableRow`.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl data.cnm test
```
