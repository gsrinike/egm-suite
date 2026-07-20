# gui.cnm.manager

`gui.cnm.manager` is the Vue.js manager UI for CNM imports.

It uses shared controls from `gui.common` and talks to `srv.cnm.services` or `mock.srv.cnm.services` through the OpenAPI-defined `/api/cnm/imports` endpoints.

Files up to 1 GB are sent as 8 MB chunks through a 16 MB Nginx request limit.
Failed imports remain in the table and expose a `Re-upload` action that retries
with the same import ID. Import rows use the `INIT`, `STORED`, and `FAILED`
states and show import-level service, timeframe, date, and user message data.
The File link opens a searchable, sortable, paginated table containing the
individual files and their profile metadata.

File rows use their own lifecycle: `INIT`, `STORED`, `PARSED`, or `FAILED`.
These values can change when downstream processing reports status updates.

The import toolbar accepts an optional message beside the RDF model selector.
That message is persisted in `ImportStatus`. The Profiles view filters
Elasticsearch metadata by profile type, TSO, business day, and business time.

The IIDM view lists transformed profile status from `srv.iidm.transformer`
without loading XIIDM payloads. Selecting a completed transform opens a dynamic
table view for that one network. Table metadata is loaded first, and rows are
requested per selected table/page so large IIDM networks are not loaded into the
browser at once.

## Local Development

Run the mock service and then:

```bash
cd gui.cnm.manager
npm install
npm run dev
```

Runtime API URLs are loaded before the Vue app mounts. Defaults live in
`public/config/base/gui.cnm.manager-application.json`; environment overrides
live in `public/config/<env>/gui.cnm.manager-application.json`, where `<env>`
defaults to `local` and can be changed through `VITE_APP_ENV`.

The config supports `cnmBaseUrl` and `iidmBaseUrl`. In Docker/Nginx,
`/api/cnm` is routed to `srv-cnm-services` and `/api/iidm` is routed to
`srv-iidm-transformer`.
