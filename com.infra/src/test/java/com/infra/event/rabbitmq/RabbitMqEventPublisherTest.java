package com.infra.event.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMqEventPublisherTest {
    @Test
    void publishesPayloadThroughRabbitTemplateConverter() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMqEventPublisher publisher = new RabbitMqEventPublisher(rabbitTemplate, new ObjectMapper());
        TestEvent payload = new TestEvent("network-a", 12);

        publisher.publish("exchange", "route", payload);

        verify(rabbitTemplate).convertAndSend("exchange", "route", payload);
    }

    private record TestEvent(String networkId, int count) {
    }
}
