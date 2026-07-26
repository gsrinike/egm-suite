package eu.egm.srv.cnm.services.config;

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
 * Declares the downstream IIDM transform queue from the CNM publisher side.
 *
 * <p>RabbitMQ drops topic messages when no queue is bound at publish time. CNM
 * publishes transform requests asynchronously after profile processing, so it
 * declares the durable IIDM request queue and binding as well as the exchange.
 * The IIDM transformer listener declares the same topology when it starts; both
 * declarations are idempotent as long as the names and durability match.</p>
 */
@Configuration
public class IidmTransformTopologyConfig {
    @Bean
    Declarables iidmTransformRequestedTopology(
            @Value("${cnm.import.event.iidm-transform-exchange:iidm.events}") String exchangeName,
            @Value("${cnm.import.event.iidm-transform-dead-letter-exchange:iidm.events.dlx}") String deadLetterExchangeName,
            @Value("${cnm.import.event.iidm-transform-routing-key:iidm.profile.transform.requested}") String routingKey,
            @Value("${cnm.import.event.iidm-transform-queue:iidm.profile.transform}") String queueName,
            @Value("${cnm.import.event.iidm-transform-dlq:iidm.profile.transform.dlq}") String deadLetterQueueName,
            @Value("${cnm.import.event.iidm-transform-dead-letter-routing-key:iidm.profile.transform.failed.dlq}") String deadLetterRoutingKey,
            @Value("${cnm.import.event.iidm-transform-started-routing-key:iidm.profile.transform.started}") String startedRoutingKey,
            @Value("${cnm.import.event.iidm-transform-started-queue:iidm.profile.transform.started.cnm}") String startedQueueName,
            @Value("${cnm.import.event.iidm-transform-completed-routing-key:iidm.profile.transform.completed}") String completedRoutingKey,
            @Value("${cnm.import.event.iidm-transform-completed-queue:iidm.profile.transform.completed.cnm}") String completedQueueName,
            @Value("${cnm.import.event.iidm-transform-failed-routing-key:iidm.profile.transform.failed}") String failedRoutingKey,
            @Value("${cnm.import.event.iidm-transform-failed-queue:iidm.profile.transform.failed.cnm}") String failedQueueName) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        TopicExchange deadLetterExchange = new TopicExchange(deadLetterExchangeName, true, false);
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(deadLetterQueueName).build();
        Queue startedQueue = QueueBuilder.durable(startedQueueName).build();
        Queue completedQueue = QueueBuilder.durable(completedQueueName).build();
        Queue failedQueue = QueueBuilder.durable(failedQueueName).build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterRoutingKey);
        Binding startedBinding = BindingBuilder.bind(startedQueue).to(exchange).with(startedRoutingKey);
        Binding completedBinding = BindingBuilder.bind(completedQueue).to(exchange).with(completedRoutingKey);
        Binding failedBinding = BindingBuilder.bind(failedQueue).to(exchange).with(failedRoutingKey);
        return new Declarables(
                exchange,
                deadLetterExchange,
                queue,
                deadLetterQueue,
                startedQueue,
                completedQueue,
                failedQueue,
                binding,
                deadLetterBinding,
                startedBinding,
                completedBinding,
                failedBinding);
    }
}
