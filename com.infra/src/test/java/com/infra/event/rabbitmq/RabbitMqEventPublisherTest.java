package com.infra.event.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMqEventPublisherTest {
    @Test
    void serializesPayloadBeforePublishing() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMqEventPublisher publisher = new RabbitMqEventPublisher(rabbitTemplate, new ObjectMapper());

        publisher.publish("exchange", "route", new TestEvent("network-a", 12));

        verify(rabbitTemplate).convertAndSend("exchange", "route", "{\"networkId\":\"network-a\",\"count\":12}");
    }

    private record TestEvent(String networkId, int count) {
    }
}
