package com.infra.event.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final ObjectMapper objectMapper;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String exchange, String routingKey, Object payload) {
        try {
            // Publish JSON text explicitly so payload classes do not need to implement Serializable.
            rabbitTemplate.convertAndSend(exchange, routingKey, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize event payload", exception);
        } catch (RuntimeException exception) {
            LOGGER.warn("Event publication failed for exchange {} and routing key {}", exchange, routingKey, exception);
            throw exception;
        }
    }
}
