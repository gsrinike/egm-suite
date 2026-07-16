package com.infra.event.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infra.event.EventPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ implementation of the generic event publisher.
 */
public class RabbitMqEventPublisher implements EventPublisherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String exchange, String routingKey, Object payload) {
        try {
            // The configured Jackson converter publishes JSON without requiring Serializable payload classes.
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (RuntimeException exception) {
            LOGGER.warn("Event publication failed for exchange {} and routing key {}", exchange, routingKey, exception);
            throw exception;
        }
    }
}
