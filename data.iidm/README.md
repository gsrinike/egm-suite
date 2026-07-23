# data.iidm

`data.iidm` is the PowSyBl-facing IIDM DTO boundary.

## Responsibilities

- Define IIDM transform request, success, and failure events.
- Represent transform diagnostics and transform state.
- Represent source CGMES files included in a transform request.
- Wrap PowSyBl `Network` values in `IidmNetworkModel`.
- Provide `IidmNetworkSummary` counts for persisted networks.
- Provide `IidmNetworkXiidm` helpers for XIIDM serialization.

This module may depend on PowSyBl. It does not own Spring services,
Elasticsearch documents, MinIO access, RabbitMQ consumers, or GUI code.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl data.iidm test
```
