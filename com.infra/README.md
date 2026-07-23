# com.infra

`com.infra` provides reusable infrastructure adapters behind small service
interfaces. Application modules use `InfrastructureUtils` instead of depending
directly on infrastructure client libraries.

## Capabilities

- `storage.document`: document repository contracts, filters, sorting, and
  paged search.
- `storage.document.elasticsearch`: Elasticsearch implementation.
- `storage.object`: object-storage contract and disabled fail-fast adapter.
- `storage.object.minio`: MinIO implementation.
- `event`: event publisher contract.
- `event.rabbitmq`: RabbitMQ implementation and topic exchange support.
- `bpm`: process start, message correlation, cancellation, and monitoring
  contracts.
- `bpm.camunda`: embedded Camunda implementation.
- `bpm.remote`: HTTP adapter for standalone BPM modules.

## Runtime Behavior

Spring configuration creates concrete adapters from `utility.*` YAML
configuration. Object storage is optional; modules that do not configure it get
a disabled adapter that fails only if invoked. RabbitMQ exchanges listed in the
module configuration are declared at startup. BPM can be embedded or remote.

RabbitMQ consumers can use the shared
`retryingRabbitListenerContainerFactory`. It applies the
`utility.messaging.listener.retry.max-attempts` policy, defaults to three
attempts, and rejects exhausted messages without requeue so RabbitMQ can route
them to the queue dead-letter exchange.

## Usage Pattern

1. A service defines a `DocumentAdapter<T>` for its document type.
2. The service asks `InfrastructureUtils` for a repository, object store, event
   publisher, or BPM service.
3. `com.infra` resolves the configured adapter.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl com.infra test
```
