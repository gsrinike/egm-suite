package com.utils.restservice;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Shared REST client wiring for modules that need outbound HTTP calls.
 *
 * <p>Runnable applications opt in by importing this configuration. It registers
 * a singleton {@link RestTemplate} with common connection and read timeouts.</p>
 */
@Configuration
public class RestServiceConfiguration {
    @Bean
    RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return new RestTemplate(requestFactory);
    }
}
