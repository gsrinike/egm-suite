package eu.egm.srv.cnm.services.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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
    Queue iidmTransformRequestedQueue(
            @Value("${cnm.import.event.iidm-transform-queue:iidm.profile.transform}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding iidmTransformRequestedBinding(
            Queue iidmTransformRequestedQueue,
            @Value("${cnm.import.event.iidm-transform-exchange:iidm.events}") String exchangeName,
            @Value("${cnm.import.event.iidm-transform-routing-key:iidm.profile.transform.requested}") String routingKey) {
        return BindingBuilder
                .bind(iidmTransformRequestedQueue)
                .to(new TopicExchange(exchangeName, true, false))
                .with(routingKey);
    }
}
