package com.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infra.document.DocumentAdapter;
import com.infra.document.DocumentRepositoryService;
import com.infra.InfrastructureAdapterFactory;
import com.infra.elasticsearch.ElasticsearchDocumentRepository;
import com.infra.event.EventPublisherService;
import com.infra.minio.MinioObjectStorageService;
import com.infra.rabbitmq.RabbitMqEventPublisher;
import com.infra.storage.ObjectStorageService;
import io.minio.MinioClient;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Spring wiring for generic infrastructure adapters.
 *
 * The properties use utility.* names intentionally so consuming services can map
 * their own domain-specific configuration onto this reusable module.
 */
@Configuration
public class InfrastructureUtilityConfig {
    @Bean
    @ConditionalOnProperty("utility.messaging.topic-exchange.name")
    TopicExchange utilityTopicExchange(@Value("${utility.messaging.topic-exchange.name}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplateCustomizer rabbitTemplateJsonCustomizer(MessageConverter jsonMessageConverter) {
        return rabbitTemplate -> rabbitTemplate.setMessageConverter(jsonMessageConverter);
    }

    @Bean
    MinioClient minioClient(
            @Value("${utility.object-storage.endpoint}") String endpoint,
            @Value("${utility.object-storage.access-key}") String accessKey,
            @Value("${utility.object-storage.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    ObjectStorageService objectStorageService(MinioClient minioClient) {
        return new MinioObjectStorageService(minioClient);
    }

    @Bean
    EventPublisherService eventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        return new RabbitMqEventPublisher(rabbitTemplate, objectMapper);
    }

    @Bean
    InfrastructureAdapterFactory infrastructureAdapterFactory(ElasticsearchOperations elasticsearchOperations,
                                                              ObjectStorageService objectStorageService,
                                                              EventPublisherService eventPublisher) {
        // Anonymous factory keeps the public extension point small while centralizing adapter construction.
        return new InfrastructureAdapterFactory() {
            @Override
            public <T> DocumentRepositoryService<T> documentRepository(DocumentAdapter<T> adapter) {
                return new ElasticsearchDocumentRepository<>(elasticsearchOperations, adapter);
            }

            @Override
            public ObjectStorageService objectStorageService() {
                return objectStorageService;
            }

            @Override
            public EventPublisherService eventPublisher() {
                return eventPublisher;
            }
        };
    }
}
