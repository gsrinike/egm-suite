package com.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infra.InfrastructureUtils;
import com.infra.bpm.BusinessProcessService;
import com.infra.bpm.DisabledBusinessProcessService;
import com.infra.bpm.remote.RemoteBusinessProcessService;
import com.infra.event.EventPublisherService;
import com.infra.event.rabbitmq.RabbitMqEventPublisher;
import com.infra.storage.document.DocumentAdapter;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.elasticsearch.ElasticsearchDocumentRepository;
import com.infra.storage.object.ObjectStorageService;
import com.infra.storage.object.minio.MinioObjectStorageService;
import io.minio.MinioClient;
import java.net.http.HttpClient;
import java.util.Arrays;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    SmartInitializingSingleton objectStorageBucketInitializer(
            ObjectStorageService objectStorageService,
            @Value("${utility.object-storage.buckets:}") String buckets) {
        return () -> Arrays.stream(buckets.split(","))
                .map(String::trim)
                .filter(bucket -> !bucket.isBlank())
                .forEach(objectStorageService::initializeBucket);
    }

    @Bean
    EventPublisherService eventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        return new RabbitMqEventPublisher(rabbitTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(BusinessProcessService.class)
    @ConditionalOnProperty("utility.bpm.remote.base-url")
    BusinessProcessService remoteBusinessProcessService(
            ObjectMapper objectMapper,
            @Value("${utility.bpm.remote.base-url}") String baseUrl) {
        return new RemoteBusinessProcessService(baseUrl, HttpClient.newHttpClient(), objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(BusinessProcessService.class)
    BusinessProcessService disabledBusinessProcessService() {
        return new DisabledBusinessProcessService();
    }

    @Bean
    InfrastructureUtils infrastructureUtils(ElasticsearchOperations elasticsearchOperations,
                                            ObjectStorageService objectStorageService,
                                            EventPublisherService eventPublisher,
                                            BusinessProcessService businessProcessService) {
        // Anonymous factory keeps the public extension point small while centralizing adapter construction.
        return new InfrastructureUtils() {
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

            @Override
            public BusinessProcessService businessProcessService() {
                return businessProcessService;
            }
        };
    }
}
