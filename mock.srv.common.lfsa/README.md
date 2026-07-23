# mock.srv.common.lfsa

`mock.srv.common.lfsa` is the in-memory mock for the common load-flow and
security-analysis API. It is used by CSA orchestration and GUI development when
the production LF/SA service is not running.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl mock.srv.common.lfsa -am test
```
