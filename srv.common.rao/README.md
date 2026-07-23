# srv.common.rao

`srv.common.rao` is the reusable remedial-action optimization REST service
boundary for CSA, capacity calculation, and operational planning workflows.

The current implementation returns deterministic recommendations and validation
metrics through `data.common` contracts.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.common.rao -am test
```
