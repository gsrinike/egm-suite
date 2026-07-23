# mock.srv.common.rao

`mock.srv.common.rao` is the in-memory mock for the common remedial-action
optimization API. It supports CSA GUI and orchestration development without the
production RAO service.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl mock.srv.common.rao -am test
```
