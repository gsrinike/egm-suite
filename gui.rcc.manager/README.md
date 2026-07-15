# gui.rcc.manager

Vue-based RCC manager. The current increment exposes CGM import management by
embedding the CNM manager screens, CSA case setup, CSA result inspection, and
workflow monitoring. CC and OPC are visible as disabled capability placeholders
for later increments.

Runtime API URLs are loaded before the Vue app mounts. Defaults live in
`public/config/base/gui.rcc.manager-application.json`; environment overrides
live in `public/config/<env>/gui.rcc.manager-application.json`, where `<env>`
defaults to `local` and can be changed through `VITE_APP_ENV`.

Default local routes use the same origin:

- `/api/cnm/**` is proxied to the CNM backend.
- `/api/csa/**` is proxied to the CSA backend.

Run locally:

```bash
npm install
npm run dev
```
