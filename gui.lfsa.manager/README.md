# gui.lfsa.manager

Vue module for Load Flow and Security Analysis screens.

The module can run standalone or be embedded by `gui.rcc.manager`. It uses the
shared components from `gui.common` and calls `srv.common.lfsa` through the
configurable `apis.lfsaBaseUrl` setting.
