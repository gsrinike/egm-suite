# gui.common Components

`gui.common` contains reusable Vue components, styles, theme helpers, refresh
controls, and browser logging utilities. Public imports should use the package
entry point:

```ts
import { AutoRefreshControl, DataTable, DynamicTable } from '@egm/gui.common/src';
```

## Button

Standard command button with shared styling.

Props:

- `type`
- `disabled`

Use for explicit commands such as import, search, retry, back, or refresh.

## Link

Lightweight clickable action styled as a link. Use for table identifiers and
detail navigation. Use `Button` for primary commands.

## Dropdown

Labelled select field for small known option sets.

Props:

- `label`
- `modelValue`
- `options`: `{ label, value }[]`

## Menu

Tab/menu selector.

Props:

- `items`: `{ id, label }[]`
- `activeId`

Events:

- `select`

## DataTable

`DataTable` renders one table when the caller knows the columns and rows.

Capabilities:

- client-side search
- client-side sort
- client-side pagination
- scrollable table area
- column cell slots

Props:

- `columns`
- `rows`
- `pageSize`
- `idKey`
- `hidePagination`
- `hideSearch`

Use `hideSearch` and `hidePagination` when the parent view performs server-side
search or paging.

## DynamicTable

`DynamicTable` renders runtime-defined table sets. It shows table tabs and uses
`DataTable` for the selected table.

Use it for profile payload exploration and IIDM network table exploration where
the backend determines table names, columns, and row counts.

Props:

- `tables`
- `loading`
- `error`
- `pageSize`
- `serverSide`
- `currentPage`

Events:

- `tableSelected(tableId)`
- `pageChange(page)`

In server-side mode the feature view owns search and paging, and the nested
`DataTable` hides its local search/pagination controls.

## DataTable Versus DynamicTable

Use `DataTable` for a single known table. Use `DynamicTable` when the table list
or columns are generated at runtime. `DynamicTable` composes `DataTable`; it is
not a replacement for all table screens.

## RefreshButton

Icon-style reload button. It emits `refresh` and can optionally reload the whole
page.

Props:

- `label`
- `title`
- `reload`

Events:

- `refresh`

## AutoRefreshControl

Refresh interval selector with `5 seconds`, `10 seconds`, `30 seconds`, and
`Manual` modes. It is used by feature screens that need user-controlled polling.

Props:

- `storageKey`
- `defaultInterval`
- `disabled`

Events:

- `intervalChange(intervalMs)`
- `refresh`

When disabled, the control emits manual mode and prevents background polling.

## Theme Helpers

Theme preference helpers persist dark/light selection so manual refreshes and
full page reloads preserve the selected theme.

## Browser Error Logging

`HttpClientError` and `logClientError` preserve failed fetch URL, HTTP status,
response body, context, and stack trace. Feature GUIs should use them in API
catch blocks so errors are visible in browser console logs and container access
logs.

## Styling

Shared styles live in `src/styles.css` and use `.common-*` class names. Feature
modules own business layout and domain labels.
