package eu.egm.srv.cgm.importer.config;

import eu.egm.data.cgm.dto.cgmes.CgmesConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class CgmImportMessagingConfig {
    public static final String IIDM_TRANSFORM_QUEUE = "cgm.iidm.transform";

    @Bean
    Queue iidmTransformQueue() {
        return new Queue(IIDM_TRANSFORM_QUEUE, true);
    }

    @Bean
    @ConditionalOnBean(TopicExchange.class)
    Binding iidmTransformBinding(Queue iidmTransformQueue, TopicExchange utilityTopicExchange) {
        return BindingBuilder.bind(iidmTransformQueue)
                .to(utilityTopicExchange)
                .with(CgmesConstants.CGMES_TRANSFORMED_ROUTING_KEY);
    }
}
