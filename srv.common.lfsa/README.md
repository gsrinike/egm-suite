# srv.common.lfsa

`srv.common.lfsa` is the reusable load-flow and security-analysis service
boundary for CSA, capacity calculation, and operational planning workflows.

It exposes compatibility REST endpoints for direct load-flow/security-analysis
requests and a CNM-driven asynchronous security-analysis run API. A run is
created for a successful CNM import, published as a RabbitMQ event, processed
against persisted IIDM network documents, and stored in the
`lfsa-security-analysis-runs` document index.

The processing path reconstructs PowSyBl `Network` objects from stored XIIDM,
uses an in-memory merge hook when a compatible PowSyBl merger is available, and
persists bounded diagnostics with the run result.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.common.lfsa -am test
```
