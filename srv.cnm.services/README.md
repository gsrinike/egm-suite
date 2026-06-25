# srv.cnm.services

`srv.cnm.services` is the Spring Boot REST service for CNM import capabilities.

## Scope

- Accept RDF/XML model files for CGM, CSA, and CC use cases.
- Support ID, 1D, and 2D timeframes.
- Extract RDF profile metadata such as `dcterms:conformsTo`.
- Classify imported payloads as CGMES, NCP, or unknown.
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
1 GB HTTP body.

Successful imports publish `cnm.import.completed` metadata events and persist one
searchable document per profile in the `cnm-profiles` Elasticsearch index.

The import aggregate transitions from `INIT` to `STORED` after all successful
RDF payloads have been parsed and written to object storage. Any file failure
sets the aggregate state to `FAILED`; profile metadata remains file-level data.

Individual files use `ImportFileState`: `INIT`, `STORED`, `PARSED`, or
`FAILED`. Downstream processors update a file through
`PUT /api/cnm/imports/{importId}/files/{fileId}/status`. The service persists
the update and recomputes the aggregate as `INIT`, `STORED`, or `FAILED`.

Elasticsearch persistence records accept schema-tolerant timestamp values and
write epoch milliseconds. The service normalizes numeric, numeric-string, ISO,
and legacy `Instant` values at the API boundary. `CnmProfileDocument` stores the
literal filename `profileType`, derived `profileFamily`, and `ImportFileState`
separately.

## Developer Commands

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true -pl srv.cnm.services -am test
```
