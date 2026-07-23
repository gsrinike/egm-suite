# srv.cnm.services

`srv.cnm.services` is the CNM import REST service. It accepts RDF/XML and ZIP
uploads, stores raw files in object storage, processes RDF metadata
asynchronously, persists CNM documents, and publishes IIDM transform events when
model groups are complete.

## Responsibilities

- Accept chunked uploads up to the configured large-file limit.
- Expand ZIP uploads and ignore platform metadata files.
- Derive business day, business time, timeframe, TSO, profile type, and profile
  family from CGMES/NCP filenames.
- Support CGMES boundary files such as `EQBD` and `TPBD`.
- Store raw payloads in MinIO through `com.infra`.
- Persist import, profile, payload, fragment, mRID, and snapshot documents in
  Elasticsearch through `com.infra`.
- Stream RDF/XML with RDF4J and extract profile-specific JSON payloads.
- Serialize processing per import/TSO/business day/business time/timeframe.
- Publish IIDM transform requests with all files needed by a model group.
- Expose OpenAPI from `src/main/resources/openapi/cnm-services.yaml`.

## Key Indices

- `cnm-imports`
- `cnm-profiles`
- `cnm-profile-payloads`
- `cnm-profile-fragments`
- `cnm-mrid-index`
- `cnm-network-snapshots`
- `cnm-network-snapshot-payloads`

## Runtime Dependencies

The service uses Elasticsearch, MinIO, RabbitMQ, `com.utils` configuration,
`com.vault` secret resolution, and `com.infra` adapters. It declares outbound
RabbitMQ exchanges it publishes to, including IIDM transform events.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.cnm.services -am test
```
