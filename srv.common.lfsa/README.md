# srv.common.lfsa

`srv.common.lfsa` is the reusable load-flow and security-analysis REST service
boundary for CSA, capacity calculation, and operational planning workflows.

The current implementation returns deterministic results through the shared
`data.common` contracts. A calculation-engine implementation can replace the
service internals without changing the CSA orchestration API shape.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.common.lfsa -am test
```
