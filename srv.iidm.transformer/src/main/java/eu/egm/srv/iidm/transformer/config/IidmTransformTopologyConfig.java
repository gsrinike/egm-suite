package eu.egm.srv.iidm.transformer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares IIDM transform queues with dead-letter handling.
 */
@Configuration
public class IidmTransformTopologyConfig {
    @Bean
    Declarables iidmTransformRequestedTopology(
            @Value("${iidm.transform.event.exchange:iidm.events}") String exchangeName,
            @Value("${iidm.transform.event.dead-letter-exchange:iidm.events.dlx}") String deadLetterExchangeName,
            @Value("${iidm.transform.event.requested-routing-key:iidm.profile.transform.requested}") String routingKey,
            @Value("${iidm.transform.event.requested-queue:iidm.profile.transform}") String queueName,
            @Value("${iidm.transform.event.requested-dlq:iidm.profile.transform.dlq}") String deadLetterQueueName,
            @Value("${iidm.transform.event.requested-dead-letter-routing-key:iidm.profile.transform.failed.dlq}") String deadLetterRoutingKey) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        TopicExchange deadLetterExchange = new TopicExchange(deadLetterExchangeName, true, false);
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(deadLetterQueueName).build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterRoutingKey);
        return new Declarables(exchange, deadLetterExchange, queue, deadLetterQueue, binding, deadLetterBinding);
    }
}
