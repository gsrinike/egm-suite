package eu.egm.srv.common.lfsa.config;

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
 * Declares LFSA queues and dead-letter topology.
 */
@Configuration
public class LfSaEventTopologyConfig {
    @Bean
    Declarables lfsaSecurityAnalysisTopology(
            @Value("${lfsa.security-analysis.event.exchange:lfsa.events}") String exchangeName,
            @Value("${lfsa.security-analysis.event.dead-letter-exchange:lfsa.events.dlx}") String deadLetterExchangeName,
            @Value("${lfsa.security-analysis.event.requested-routing-key:lfsa.security-analysis.requested}") String routingKey,
            @Value("${lfsa.security-analysis.event.requested-queue:lfsa.security-analysis}") String queueName,
            @Value("${lfsa.security-analysis.event.requested-dlq:lfsa.security-analysis.dlq}") String deadLetterQueueName,
            @Value("${lfsa.security-analysis.event.requested-dead-letter-routing-key:lfsa.security-analysis.failed.dlq}") String deadLetterRoutingKey) {
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
