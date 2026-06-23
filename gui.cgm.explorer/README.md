# gui.cgm.explorer

## Purpose

`gui.cgm.explorer` is the React frontend for importing CGMES data, starting the CGM import BPM process, visualizing process/file status history, selecting imported network ids, exploring indexed equipment, and comparing network states.

It is built with React, TypeScript, Vite, and lucide-react icons. Maven wraps the npm lifecycle so the GUI can be built as part of the monorepo.

## What It Contains

- `src/App.tsx`
  - Main application shell.
  - Loads import history.
  - Starts the CGM import BPM process for `Init` imports.
  - Displays process instance id and per-file import/IIDM status history.
  - Provides the network-id selector.
  - Hosts the `Explore` and `Compare` tabs.
- `src/components/ImportPanel.tsx`
  - Upload form.
  - Lets users select region, process, and CGMES files.
  - Business day and timestamp are no longer manual inputs; they are parsed by the backend from filenames.
- `src/components/EquipmentExplorer.tsx`
  - Searchable equipment table.
  - Supports filters for equipment type, business day, timestamp, region, process, time frame, TSO, CGMES profile, version, and extension.
- `src/components/NetworkCompare.tsx`
  - Separate comparison tab for comparing two imported network ids.
- `src/services/cgmApi.ts`
  - Typed API client for backend REST calls.
- `src/styles.css`
  - Application layout and component styling.
- `nginx.conf`
  - Runtime Nginx configuration for the Docker image.
  - Proxies `/api/*` calls to the backend service.

## API Usage

The GUI calls:

- `POST /api/cgm/imports`
- `GET /api/cgm/imports`
- `POST /api/cgm/imports/processes/start`
- `GET /api/cgm/imports/{networkId}/process-history`
- `GET /api/cgm/networks/{networkId}/equipment`
- `GET /api/cgm/networks/{leftNetworkId}/compare/{rightNetworkId}`

In Docker, Nginx proxies `/api/*` to `http://srv-cgm-importer:8080`.

## Developer Commands

Run locally with Vite:

```bash
npm install
npm run dev
```

Run tests:

```bash
npm test
```

Build static assets:

```bash
npm run build
```

Build through Maven from the repository root:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true -pl gui.cgm.explorer test
```

## Docker Image

The Docker image builds the Vite output and serves it with Nginx.

Through Maven:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.push=true -pl gui.cgm.explorer package
```

## Implementation Notes

The explorer and comparison views are separate tabs. This gives the equipment table full width and avoids layout overlap as filters and result columns grow.

The network id input is backed by the persisted import list returned from the backend, so developers should treat import history as the source of selectable network states.
