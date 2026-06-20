# com.infra

## Purpose

`com.infra` is a reusable backend infrastructure utility module. It owns technology-specific integration with Elasticsearch, MinIO, and RabbitMQ, and exposes small application-facing abstractions so service modules do not depend directly on those technologies.

Service modules depend on this module for persistence, object storage, and event publishing.

## What It Contains

### Document Abstractions

- `DocumentAdapter<T>`: application-provided adapter that tells the utility layer the index name, document id, and document class.
- `DocumentRepositoryService<T>`: storage abstraction for saving, listing, and searching documents.
- `DocumentFilter`: exact or contains filter used by document search.
- `DocumentSearchRequest`: paged search request containing exact filters and any-match filters.
- `DocumentPage<T>`: paged document search response with total hit count.
- `DocumentSort`: storage-neutral sort description.
- `InfrastructureAdapterFactory`: factory in package `com.infra` used by application modules to resolve adapters.

### Technology Adapters

- `ElasticsearchDocumentRepository<T>`: Spring Data Elasticsearch implementation of `DocumentRepositoryService<T>`.
- `MinioObjectStorageService`: MinIO implementation of `ObjectStorageService`.
- `RabbitMqEventPublisher`: RabbitMQ implementation of `EventPublisherService`.

### Spring Configuration

- `InfrastructureUtilityConfig`: registers infrastructure beans, including:
  - optional RabbitMQ topic exchange from `utility.messaging.topic-exchange.name`
  - RabbitMQ JSON message converter
  - MinIO client
  - object storage adapter
  - event publisher adapter
  - infrastructure adapter factory

## Implementation Notes

This module uses a factory/adapter pattern:

1. A service module creates a small `DocumentAdapter<T>` for its own document type.
2. The service module asks `InfrastructureAdapterFactory` for a `DocumentRepositoryService<T>`.
3. The utility module resolves the concrete Elasticsearch implementation.

This keeps service modules isolated from Elasticsearch classes while still allowing module-specific document shapes.

Paged filters are executed inside Elasticsearch. This matters for large imports because callers should not load the first N records and then filter in memory.

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

Application-specific parsing, business concepts, queue names, index names, and REST controllers should not be added here. Those belong in the relevant consuming module.
