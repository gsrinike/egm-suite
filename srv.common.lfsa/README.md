# srv.common.lfsa

`srv.common.lfsa` is the reusable load-flow and security-analysis service
boundary for CSA, capacity calculation, and operational planning workflows.

It exposes compatibility REST endpoints for direct load-flow/security-analysis
requests and a CNM-driven asynchronous Load Flow & Security Analysis run API. A run is
created for a successful CNM import, published as a RabbitMQ event, processed
against persisted IIDM network documents, and stored in the
`lfsa-security-analysis-runs` document index.

The processing path reconstructs PowSyBl `Network` objects from stored XIIDM,
uses an in-memory merge hook when a compatible PowSyBl merger is available, and
persists bounded diagnostics with the run result.

LFnSA defaults are loaded lazily and cached from
`src/main/resources/config/security-analysis/default.yaml`. The YAML contains
the load-flow strategy, load-flow parameters, security-analysis parameters, and
contingency generation defaults. Named configuration sets are stored in
`lfsa-security-analysis-parameters`; each run stores the selected configuration
snapshot and the bounded PowSyBl result projection in
`lfsa-security-analysis-runs`.

The asynchronous run path reconstructs and merges IIDM networks, runs PowSyBl
`LoadFlow.run(...)` first using the configured strategy (`DC_ONLY`, `AC_ONLY`,
or `AC_WITH_DC_FAILOVER`), aborts if no load-flow attempt converges, builds N-1
contingencies from the converged network, maps the stored DTOs to PowSyBl
`LoadFlowParameters` and `SecurityAnalysisParameters`, and invokes
`SecurityAnalysis.run(...)`.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.common.lfsa -am test
```
