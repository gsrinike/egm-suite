# data.iidm

`data.iidm` contains PowSyBl-based IIDM network wrappers and transform event
contracts used by EGM services. The canonical IIDM model is
`com.powsybl.iidm.network.Network`.

The module intentionally depends on PowSyBl and remains independent from Spring,
Elasticsearch, RabbitMQ, MinIO, and frontend code. Runtime services own
infrastructure and API behavior; this module owns the grid model contracts.

Key types:

- `IidmNetworkModel`: transformation result backed by a real PowSyBl `Network`.
- `IidmNetworkSummary`: serializable counts and metadata derived from `Network`.
- `IidmNetworkXiidm`: XIIDM read/write helpers for persistence and exchange.
- `IidmProfileTransformRequested`, `IidmProfileTransformCompleted`, and
  `IidmProfileTransformFailed`: event contracts for the transform workflow.
