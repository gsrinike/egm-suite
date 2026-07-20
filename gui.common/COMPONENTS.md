# gui.common Reusable Components

`gui.common` owns domain-neutral Vue components, shared browser utilities, and
CSS classes that can be reused by GUI modules such as `gui.cnm.manager` and
`gui.rcc.manager`. Components in this module must not reference CNM, CSA, IIDM,
or other business-domain names in their public labels, props, or CSS class
names.

## Export Surface

All public components and utilities are exported from `src/index.ts`.
Consuming modules should import from the package entry point:

```ts
import { Button, DataTable, DynamicTable, RefreshButton } from '@egm/gui.common/src';
```

Avoid importing component files directly from another GUI module. Keeping the
entry point stable makes future refactoring easier.

## Button

`Button` is the standard command button. It wraps a native `button` element and
uses the shared `.common-button` styling.

Use it for explicit user commands such as import, search, refresh, retry, or
back actions.

Props:

- `type`: native button type. Defaults to `button`.
- `disabled`: disables the command and applies shared disabled styling.

## Link

`Link` renders a lightweight clickable action styled as a link.

Use it when an item in a table opens a detail view, such as "View files" or a
clickable file/network identifier. Do not use it for primary commands; use
`Button` for those.

## Dropdown

`Dropdown` renders a labelled select field.

Use it for small, known option sets such as service type, timeframe, mode, or
successful import selection. The component is domain-neutral; callers provide
all option labels and values.

Props:

- `label`: visible field label.
- `modelValue`: selected value.
- `options`: array of `{ label, value }`.

## Menu

`Menu` renders a horizontal tab/menu selector.

Use it for top-level views inside a GUI module, such as Imports, Profiles, IIDM,
or workflow tabs. The component owns the generic active-item behavior but does
not own any route or business logic.

Props:

- `items`: array of `{ id, label }`.
- `activeId`: selected item id.

Events:

- `select`: emitted with the selected item id.

## DataTable

`DataTable` is the base table engine for one flat table with known columns.

Use it when the caller already knows the columns and has a list of rows, for
example:

- import lists
- file lists
- profile metadata lists
- IIDM transform status lists

Capabilities:

- in-table search across all row values
- client-side sorting
- client-side pagination
- scrollable table area
- named cell slots for custom cell content

Props:

- `columns`: array of `{ key, label }`.
- `rows`: flat array of row objects.
- `pageSize`: rows per client-side page. Defaults to `10`.
- `idKey`: row identifier field. Defaults to `id`.
- `hidePagination`: hides the built-in client-side pager. Use this only when a
  parent component owns paging, such as `DynamicTable` in server-side mode.
- `hideSearch`: hides the built-in client-side search. Use this for server-side
  datasets where the parent component sends search text to the backend.

Slots:

- `cell-<columnKey>`: custom renderer for a column cell.

Example:

```vue
<DataTable :columns="columns" :rows="rows" id-key="importId">
  <template #cell-file="{ row }">
    <Link @click="openFiles(String(row.importId))">View files</Link>
  </template>
</DataTable>
```

## DynamicTable

`DynamicTable` is a higher-level table explorer for data whose table shape is
generated at runtime. It renders table tabs and delegates the active table to
`DataTable`.

Use it when a backend returns multiple generated table definitions, for example:

- RDF profile payload contents
- IIDM network details such as Buses, Lines, Loads, and Element Counts

`DynamicTable` should be preferred over building ad-hoc table tabs in feature
modules. It keeps generated-table behavior consistent across GUI applications.

Props:

- `tables`: array of dynamic table definitions.
- `loading`: displays loading state.
- `error`: displays an error message.
- `pageSize`: page size passed to the active table.
- `serverSide`: when `true`, the nested `DataTable` hides its own search and
  pagination, and `DynamicTable` emits page/table events. The parent feature
  view owns server-side search controls and passes the search term to its API.
- `currentPage`: current server-side page index.

Events:

- `tableSelected(tableId)`: emitted when the user selects a dynamic table tab.
- `pageChange(page)`: emitted when the user requests the previous or next page
  in server-side mode.

### DynamicTable Versus DataTable

`DataTable` renders one known table. `DynamicTable` renders one of many
runtime-defined tables.

`DynamicTable` uses `DataTable` internally. In local mode the base table remains
responsible for row rendering, search, sorting, and regular client-side
pagination. In server-side mode the feature view owns search and paging because
only the backend has all rows for the selected table.

## RefreshButton

`RefreshButton` is a shared icon-style reload control.

By default it emits `refresh` and then calls `window.location.reload()`. Use it
when a screen needs a full browser reload to refresh app configuration or clear
stale status held in browser state.

Props:

- `label`: accessible label. Defaults to `Reload page`.
- `title`: hover title. Defaults to `Reload page`.
- `reload`: when `false`, the component only emits `refresh` and does not reload
  the page.

Events:

- `refresh`: emitted before the optional reload.

## Browser Error Logging

`logClientError` and `HttpClientError` live under `src/logging`.

Use `HttpClientError.fromResponse(...)` for failed `fetch` calls so the HTTP
status, URL, and response body are preserved. Use `logClientError(...)` in catch
blocks so browser/container logs contain a useful stack trace and context.

## Styling Rules

Shared CSS lives in `src/styles.css` and uses `.common-*` class names. Feature
modules may define their own layout classes, but common component styling should
remain here when it applies to more than one GUI module.

Keep components visually compatible with dark and light themes by using CSS
variables such as:

- `--bg-card`
- `--border-color`
- `--primary`
- `--accent`
- `--text-main`
- `--button-text`

Do not hard-code domain-specific colors or copy module-specific CSS into common
components unless it is genuinely reusable.
