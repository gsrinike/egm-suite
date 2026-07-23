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
 * Declares CNM processing queues with dead-letter queues.
 */
@Configuration
public class CnmEventTopologyConfig {
    @Bean
    Declarables cnmFileProcessingTopology(
            @Value("${cnm.import.event.exchange:cnm.events}") String exchangeName,
            @Value("${cnm.import.event.dead-letter-exchange:cnm.events.dlx}") String deadLetterExchangeName,
            @Value("${cnm.import.event.file-processing-routing-key:cnm.file.processing.requested}") String routingKey,
            @Value("${cnm.import.event.file-processing-queue:cnm.file.process}") String queueName,
            @Value("${cnm.import.event.file-processing-dlq:cnm.file.process.dlq}") String deadLetterQueueName,
            @Value("${cnm.import.event.file-processing-dead-letter-routing-key:cnm.file.processing.failed}") String deadLetterRoutingKey) {
        return retryableQueueTopology(
                exchangeName,
                deadLetterExchangeName,
                routingKey,
                queueName,
                deadLetterQueueName,
                deadLetterRoutingKey);
    }

    @Bean
    Declarables cnmSnapshotAssemblyTopology(
            @Value("${cnm.import.event.exchange:cnm.events}") String exchangeName,
            @Value("${cnm.import.event.dead-letter-exchange:cnm.events.dlx}") String deadLetterExchangeName,
            @Value("${cnm.import.event.snapshot-assembly-routing-key:cnm.snapshot.assembly.requested}") String routingKey,
            @Value("${cnm.import.event.snapshot-assembly-queue:cnm.snapshot.assemble}") String queueName,
            @Value("${cnm.import.event.snapshot-assembly-dlq:cnm.snapshot.assemble.dlq}") String deadLetterQueueName,
            @Value("${cnm.import.event.snapshot-assembly-dead-letter-routing-key:cnm.snapshot.assembly.failed}") String deadLetterRoutingKey) {
        return retryableQueueTopology(
                exchangeName,
                deadLetterExchangeName,
                routingKey,
                queueName,
                deadLetterQueueName,
                deadLetterRoutingKey);
    }

    private Declarables retryableQueueTopology(
            String exchangeName,
            String deadLetterExchangeName,
            String routingKey,
            String queueName,
            String deadLetterQueueName,
            String deadLetterRoutingKey) {
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
