# data.common

`data.common` owns framework-neutral DTOs shared by the RCC manager and the
common load-flow, security-analysis, and sensitivity-analysis services.

## Contents

- `eu.egm.data.common.lfsa.common`: common paging, timeframe, network case
  references, load-flow parameters/results, security-analysis parameters/results,
  contingency violations, and LF/SA run state.
- `eu.egm.data.common.lfsa.sensitivity`: sensitivity-analysis configurations,
  run summaries/details, uploaded PTDF/LODF/GLSK table views, and matrix rows.

The module is framework-neutral and does not depend on Spring, Camunda,
Elasticsearch, MinIO, RabbitMQ, or calculation-engine libraries.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl data.common test
```
