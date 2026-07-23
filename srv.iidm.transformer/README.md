# srv.iidm.transformer

`srv.iidm.transformer` consumes IIDM transform events and persists PowSyBl IIDM
network results for GUI exploration and downstream analysis.

## Responsibilities

- Consume `IidmProfileTransformRequested` events.
- Read raw CGMES source files from object storage.
- Stage grouped source files, including boundary files, for PowSyBl.
- Convert CGMES directly to PowSyBl `Network` through `map.cnm.iidm`.
- Preserve compatibility conversion from CNM profile payloads or snapshots.
- Capture bounded PowSyBl conversion diagnostics.
- Persist transform state in `iidm-profile-transforms`.
- Persist XIIDM and JSON table projection in `iidm-networks`.
- Expose paged/searchable transform and network table APIs.

## Storage

`iidm-profile-transforms` contains transform status, message, diagnostics,
timestamps, source linkage, and network ID. `iidm-networks` contains source file
IDs, metadata, element counts, XIIDM payload, and GUI-oriented JSON table
projection. List APIs exclude heavy payload fields.

## Runtime Dependencies

The service uses Elasticsearch, MinIO, RabbitMQ, PowSyBl, `com.utils`,
`com.vault`, `com.mapping`, and `com.infra`.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.iidm.transformer -am test
```
