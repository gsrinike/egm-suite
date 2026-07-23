# srv.iidm.transformer

`srv.iidm.transformer` consumes profile transform events and creates PowSyBl
IIDM networks from parsed CNM profile payloads.

Storage ownership:

- `iidm-profile-transforms`: transform state, diagnostics, and linkage by `fileId`.
- `iidm-networks`: profile-level PowSyBl network exports in XIIDM format, plus
  searchable network counts and source metadata.

The service reads CNM profile payloads from `cnm-profile-payloads` by ID but does
not own CNM import/profile metadata. It does not use MinIO/object storage, so its
infra configuration only needs Elasticsearch and RabbitMQ settings.

IIDM mapping defaults are loaded once and cached through `com.utils` from
`src/main/resources/config/profile/iidm/defaults.yml`. This keeps conversion
defaults such as fallback nominal voltage, fallback containers, and line
reactance out of Java source while remaining thread-safe for parallel transform
events.

List APIs return lightweight metadata and exclude XIIDM payload fields. GUI
table exploration is lazy: `/api/iidm/networks/{networkId}/tables` returns table
metadata, and `/api/iidm/networks/{networkId}/tables/{tableId}` returns one
paged table at a time.
