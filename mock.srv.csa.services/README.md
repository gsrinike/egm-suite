# mock.srv.csa.services

`mock.srv.csa.services` is the in-memory mock for the CSA orchestration API. It
returns deterministic workflow, LF/SA, and RAO-style results for GUI
development.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl mock.srv.csa.services -am test
```
