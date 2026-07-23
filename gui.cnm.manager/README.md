# gui.cnm.manager

`gui.cnm.manager` is the standalone Vue UI for CNM import and exploration.

## Screens

- Imports: create imports, upload RDF/XML or ZIP files, provide an import
  message, view aggregate status, and open file details.
- File details: view per-file status, profile type, TSO, business day/time, and
  retry options where applicable.
- Profiles: select a successful import, view profile metadata, and open dynamic
  profile payload tables.
- IIDM: select a successful import, view IIDM transform status, and lazily open
  selected network tables.

Large uploads are chunked in the browser. Shared tables, dynamic tables,
refresh controls, theme persistence, and error logging come from `gui.common`.

## Runtime Configuration

Configuration is loaded before the Vue app mounts:

```text
public/config/base/gui.cnm.manager-application.json
public/config/<env>/gui.cnm.manager-application.json
```

`VITE_APP_ENV` selects the environment and defaults to `local`. The config
contains CNM and IIDM base URLs.

## Developer Commands

```bash
cd gui.cnm.manager
npm install
npm run dev
```
