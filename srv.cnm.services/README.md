# srv.cnm.services

`srv.cnm.services` is the Spring Boot REST service for CNM import capabilities.

## Scope

- Accept RDF/XML model files for CGM, CSA, and CC use cases.
- Support ID, 1D, and 2D timeframes.
- Extract RDF profile metadata such as `dcterms:conformsTo` asynchronously after object storage succeeds.
- Stream RDF/XML through RDF4J Rio into compact RDF facts before mapping those facts to profile DTOs.
- Classify imported payloads as CGMES, NCP, or unknown.
- Resolve profile kinds through `CgmesProfileKind` and `NCProfileKind`; `ProfileFamily` remains only `CGMES`, `NCP`, or `Unknown`.
- Store raw RDF payloads through `com.infra.storage.object`.
- Persist import metadata through `com.infra.storage.document`.
- Record rejected or failed uploads as `FAILED` imports and support re-upload under the same import ID.
- Persist the optional user message supplied with an import.
- Publish OpenAPI under `src/main/resources/openapi/cnm-services.yaml`.

## Runtime Configuration

The module sets `module=srv.cnm.services` and loads YAML configuration through `com.utils`.

Required local infrastructure is Elasticsearch and MinIO. The default MinIO secret is resolved through `${vault:MINIO_SECRET_KEY}` with a local fallback in the base vault configuration.

The GUI supports files up to 1 GB by sending 8 MB binary chunks. Nginx and Spring
accept 16 MB per request, leaving headroom around each chunk without buffering a
1 GB HTTP body. `.rdf`, `.xml`, and `.idm` entries are accepted from direct
uploads or nested ZIP bundles.

Successful imports first persist every raw RDF/XML payload to object storage and
then publish one `cnm.file.processing.requested` event per stored file. The
worker consumes those events, extracts profile metadata, and persists one
searchable metadata document per profile in the `cnm-profiles` Elasticsearch
index. Large typed profile JSON is stored separately in `cnm-profile-payloads`
by `fileId`, so profile search/list screens do not load payload data into the
service heap.

The worker also stores a compact `ProfileFragment` JSON document in
`cnm-profile-fragments` and mRID lookup rows in `cnm-mrid-index`. Once every
file in the same import, TSO, business day, business time, and timeframe group
is parsed, file processing publishes one `IidmProfileTransformRequested` for
each parsed `ProfileProcessingContext`, then publishes
`CnmSnapshotAssemblyRequested` and returns. A separate snapshot listener stitches
those fragments into a `CgmNetworkSnapshot` using a two-pass mRID resolution
pipeline. Snapshot metadata is stored in `cnm-network-snapshots`; the large
sectioned JSON payload is stored in `cnm-network-snapshot-payloads`. Only after
those payload sections are stored is the snapshot marked `DONE` and an
additional IIDM transform request published with the snapshot ID. If payload
persistence fails, the snapshot is marked failed while the parsed import and
profile payload remain available for diagnostics.

File-processing events are serialized per import, TSO, business day, business
time, and timeframe inside the service. This keeps cross-referenced EQ, TP, SSH,
and SV files for the same model group from being parsed against changing import
state concurrently, while still allowing unrelated TSOs and timeframes to
process in parallel. The serialization key and extraction metadata are carried
by `ProfileProcessingContext`, which is passed from the import processor to the
RDF metadata extractor.

Profile extraction defaults are loaded from cached YAML resources under
`src/main/resources/config/profile/cgmes` and
`src/main/resources/config/profile/nc`. The extractor adds diagnostics when a
processed profile kind is outside the configured supported-kind list.

After profile payload and snapshot persistence, CNM also publishes IIDM transform requests to
`iidm.events`. The service declares `iidm.events` through
`utility.messaging.topic-exchange.additional-names` so RabbitMQ accepts the
publish even when the IIDM transformer worker starts later.

The import aggregate transitions from `INIT` to `STORED` after all successful
RDF payloads have been written to object storage and queued for metadata
processing. Once every file is parsed, the aggregate becomes `SUCCESS`. Any file
failure sets the aggregate state to `FAILED`; profile metadata remains file-level data.

Individual files use `ImportFileState`: `INIT`, `STORED`, `PARSED`, or
`FAILED`. Downstream processors update a file through
`PUT /api/cnm/imports/{importId}/files/{fileId}/status`. The service persists
the update and recomputes the aggregate as `INIT`, `STORED`, `SUCCESS`, or
`FAILED`.

Elasticsearch persistence records accept schema-tolerant timestamp values and
write epoch milliseconds. The service normalizes numeric, numeric-string, ISO,
and legacy `Instant` values at the API boundary. `CnmProfileDocument` stores the
literal filename `profileType`, derived `profileFamily`, and `ImportFileState`
separately.

The Docker image compiles `srv.cnm.services` and its Maven dependencies from
the current source tree. It does not copy a previously built local JAR. Startup
validates that persisted `createdAt` and `uploadedAt` record components use the
schema-tolerant `Object` type.

## Developer Commands

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true -pl srv.cnm.services -am test
```
