# mock.srv.cnm.services

`mock.srv.cnm.services` exposes the same initial CNM import REST paths as `srv.cnm.services` with in-memory data.

It is intended for GUI work when Elasticsearch, MinIO, and other infrastructure are not running.

## Developer Commands

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true -pl mock.srv.cnm.services -am test
```
