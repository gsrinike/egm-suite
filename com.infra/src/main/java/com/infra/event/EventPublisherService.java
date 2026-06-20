package com.infra.event;

/**
 * Generic event-publishing boundary. Callers provide exchange/routing names,
 * while implementations handle serialization and transport details.
 */
public interface EventPublisherService {
    void publish(String exchange, String routingKey, Object payload);
}
