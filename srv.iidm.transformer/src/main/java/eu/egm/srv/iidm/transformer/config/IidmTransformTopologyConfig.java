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
            @Value("${iidm.transform.event.requested-dead-letter-routing-key:iidm.profile.transform.failed.dlq}") String deadLetterRoutingKey,
            @Value("${iidm.transform.event.merge-requested-routing-key:iidm.network.merge.requested}") String mergeRoutingKey,
            @Value("${iidm.transform.event.merge-requested-queue:iidm.network.merge}") String mergeQueueName,
            @Value("${iidm.transform.event.merge-requested-dlq:iidm.network.merge.dlq}") String mergeDeadLetterQueueName,
            @Value("${iidm.transform.event.merge-requested-dead-letter-routing-key:iidm.network.merge.failed.dlq}") String mergeDeadLetterRoutingKey) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        TopicExchange deadLetterExchange = new TopicExchange(deadLetterExchangeName, true, false);
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(deadLetterQueueName).build();
        Queue mergeQueue = QueueBuilder.durable(mergeQueueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(mergeDeadLetterRoutingKey)
                .build();
        Queue mergeDeadLetterQueue = QueueBuilder.durable(mergeDeadLetterQueueName).build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterRoutingKey);
        Binding mergeBinding = BindingBuilder.bind(mergeQueue).to(exchange).with(mergeRoutingKey);
        Binding mergeDeadLetterBinding =
                BindingBuilder.bind(mergeDeadLetterQueue).to(deadLetterExchange).with(mergeDeadLetterRoutingKey);
        return new Declarables(
                exchange,
                deadLetterExchange,
                queue,
                deadLetterQueue,
                mergeQueue,
                mergeDeadLetterQueue,
                binding,
                deadLetterBinding,
                mergeBinding,
                mergeDeadLetterBinding);
    }
}
