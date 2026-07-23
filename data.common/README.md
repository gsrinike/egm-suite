# data.common

`data.common` owns DTOs shared by RCC, CSA, common LF/SA, common RAO, BPM,
GUI, and mock modules.

## Contents

- network case references
- load-flow requests and results
- security-analysis requests and results
- contingency violations
- RAO requests, results, and remedial actions
- CSA case status
- workflow instance and task views
- common paging and timeframe values

The module is framework-neutral and does not depend on Spring, Camunda,
Elasticsearch, MinIO, RabbitMQ, or calculation-engine libraries.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl data.common test
```
