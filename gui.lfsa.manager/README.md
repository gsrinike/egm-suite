# gui.lfsa.manager

Vue module for Load Flow & Security Analysis screens.

The module can run standalone or be embedded by `gui.rcc.manager`. It uses the
shared components from `gui.common` and calls `srv.common.lfsa` through the
configurable `apis.lfsaBaseUrl` setting.

The Load Flow & Security Analysis screen has three tabs:
- Search eligible successful imports and start a run with either default or
  saved LFnSA configuration using the `Run LFnSA` action.
- LFnSA Configuration for editing and saving named PowSyBl Load Flow and
  Security Analysis configurations. The load-flow strategy controls whether
  the pre-check runs in DC mode, AC mode, or AC with DC failover.
- LFnSA Results for run lookup and table-based result inspection.
