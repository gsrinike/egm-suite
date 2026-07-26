# gui.rcc.manager

`gui.rcc.manager` is the Vue UI for RCC operations.

## Current Scope

- Sidebar navigation for CGM, CSA, CC, OPC, and workflow monitoring.
- CGM > Import Manager embeds the CNM manager view and keeps CNM/IIDM backend
  URLs configurable.
- CGM > Security Analysis embeds `gui.lfsa.manager` for searching successful
  imports, starting LFSA runs, and viewing persisted run results.
- CGM > Sensitivity Analysis is present as a disabled placeholder for a later
  increment.
- The embedded CNM view includes file-level IIDM aggregate status and links to
  the IIDM transformation view.
- CSA screens call CSA/common service APIs.
- Workflow Monitor visualizes workflow instances.
- CC and OPC are displayed as inactive placeholders.

The UI uses `gui.common` for shared components, theme persistence, refresh
controls, dynamic tables, and browser-side error logging.

## Runtime Configuration

Configuration is loaded before mount:

```text
public/config/base/gui.rcc.manager-application.json
public/config/<env>/gui.rcc.manager-application.json
```

`VITE_APP_ENV` selects the environment and defaults to `local`. The config
contains CNM, IIDM, CSA, LF/SA, and RAO base URLs.

## Developer Commands

```bash
cd gui.rcc.manager
npm install
npm run dev
```
