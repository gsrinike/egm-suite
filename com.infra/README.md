# com.infra

## Purpose

`com.infra` is a reusable backend infrastructure utility module. It owns technology-specific integration with Elasticsearch, MinIO, RabbitMQ, and Camunda, and exposes small application-facing abstractions so service modules do not depend directly on those technologies.

Service modules depend on this module for persistence, object storage, event publishing, and process orchestration.

## What It Contains

### Document Abstractions

- `storage.document.DocumentAdapter<T>`: application-provided adapter that tells the utility layer the index name, document id, and document class.
- `storage.document.DocumentRepositoryService<T>`: storage abstraction for saving, listing, and searching documents.
- `storage.document.DocumentFilter`: exact or contains filter used by document search.
- `storage.document.DocumentSearchRequest`: paged search request containing exact filters and any-match filters.
- `storage.document.DocumentPage<T>`: paged document search response with total hit count.
- `storage.document.DocumentSort`: storage-neutral sort description.
- `InfrastructureUtils`: factory in package `com.infra` used by application modules to resolve adapters.

### Technology Adapters

- `storage.document.elasticsearch.ElasticsearchDocumentRepository<T>`: Spring Data Elasticsearch implementation of `DocumentRepositoryService<T>`.
- `storage.object.ObjectStorageService`: object-storage abstraction.
- `storage.object.minio.MinioObjectStorageService`: MinIO implementation of `ObjectStorageService`.
- `event.EventPublisherService`: event-publishing abstraction.
- `event.rabbitmq.RabbitMqEventPublisher`: RabbitMQ implementation of `EventPublisherService`.
- `bpm.BusinessProcessService`: BPM abstraction for starting, canceling, correlating messages with, and monitoring business process instances.
- `bpm.ProcessStartRequest`, `ProcessInstance`, `ProcessInstanceStatus`, `ProcessMessage`, and `ProcessMessageResult`: storage-neutral BPM request and response contracts.
- `bpm.camunda.CamundaBusinessProcessService`: Camunda 8 adapter backed by Zeebe for process commands and optional Operate HTTP lookup for monitoring.
- `bpm.DisabledBusinessProcessService`: default no-op monitor and fail-fast command adapter used when BPM is not enabled.

### Spring Configuration

- `InfrastructureUtilityConfig`: registers infrastructure beans, including:
  - optional RabbitMQ topic exchange from `utility.messaging.topic-exchange.name`
  - RabbitMQ JSON message converter
  - MinIO client
  - object storage adapter
  - event publisher adapter
  - optional Camunda Zeebe client from `utility.bpm.camunda.*`
  - BPM adapter, using Camunda when enabled or the disabled adapter otherwise
  - infrastructure utility factory

Example Camunda configuration:

```yaml
utility:
  bpm:
    camunda:
      enabled: true
      zeebe:
        gateway-address: localhost:26500
        plaintext: true
      operate:
        base-url: http://localhost:8081
        bearer-token: "${vault:CAMUNDA_OPERATE_TOKEN}"
```

`operate` settings are optional. Without `utility.bpm.camunda.operate.base-url`, process commands still use Zeebe, while monitor lookups return an empty result.

## Implementation Notes

This module uses a utility factory/adapter pattern:

1. A service module creates a small `DocumentAdapter<T>` for its own document type.
2. The service module asks `InfrastructureUtils` for a `DocumentRepositoryService<T>`.
3. The utility module resolves the concrete Elasticsearch implementation.

This keeps service modules isolated from Elasticsearch classes while still allowing module-specific document shapes.

Paged filters are executed inside Elasticsearch. This matters for large imports because callers should not load the first N records and then filter in memory.

The same boundary is used for BPM. Service modules call `InfrastructureUtils.businessProcessService()` and depend only on `BusinessProcessService`. Camunda-specific Zeebe and Operate classes remain inside `com.infra.bpm.camunda`.

## Developer Commands

From the repository root:

```bash
mvn -Dmaven.repo.local=work/m2 -pl com.infra test
```

To compile it with dependent backend modules:

Run the consuming service module test command from that module's documentation.

## Dependency Rules

Technology dependencies belong here when they are common service infrastructure:

- Elasticsearch
- MinIO
- RabbitMQ
- Camunda

Application-specific parsing, business concepts, queue names, index names, and REST controllers should not be added here. Those belong in the relevant consuming module.
