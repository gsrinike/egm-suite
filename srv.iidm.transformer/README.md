# srv.iidm.transformer

`srv.iidm.transformer` consumes IIDM transform events and creates PowSyBl IIDM
networks. The preferred event carries `sourceFiles`, a grouped list of raw CGMES
objects stored by `srv.cnm.services` after all cross-referenced files for a
model group are parsed. The service stages those source objects in a temporary
workspace and delegates conversion to PowSyBl's native CIM-CGMES importer.

Storage ownership:

- `iidm-profile-transforms`: transform state, diagnostics, and linkage by `fileId`.
- `iidm-networks`: profile-level PowSyBl network exports in XIIDM format, plus
  searchable network counts and source metadata.

The direct path reads raw CGMES source bytes from object storage. The service
can still read legacy snapshot metadata from `cnm-network-snapshots` and
per-file profile payloads from `cnm-profile-payloads` when a compatibility event
does not provide direct `sourceFiles`.

IIDM and PowSyBl CGMES import defaults are loaded once and cached through
`com.utils` from `src/main/resources/config/profile/iidm/defaults.yml`. This
keeps values such as `iidm.import.cgmes.source-for-iidm-id`, subnetwork
handling, SV injection conversion, and compatibility fallback defaults out of
Java source while remaining thread-safe for parallel transform events.

List APIs return lightweight metadata and exclude XIIDM payload fields. GUI
table exploration is lazy: `/api/iidm/networks/{networkId}/tables` returns table
metadata, and `/api/iidm/networks/{networkId}/tables/{tableId}` returns one
paged table at a time.
