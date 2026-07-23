# mock.srv.cnm.services

`mock.srv.cnm.services` is an in-memory implementation of the CNM REST shape.
It supports GUI development without Elasticsearch, MinIO, or RabbitMQ.

The mock mirrors the production API shape closely enough for upload status,
file-list, profile-list, and profile-table UI work.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl mock.srv.cnm.services -am test
```
