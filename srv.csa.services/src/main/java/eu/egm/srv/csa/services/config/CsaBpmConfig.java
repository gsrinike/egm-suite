package eu.egm.srv.csa.services.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infra.bpm.BusinessProcessService;
import com.infra.bpm.DisabledBusinessProcessService;
import com.infra.bpm.remote.RemoteBusinessProcessService;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CsaBpmConfig {
    @Bean
    @ConditionalOnProperty("csa.bpm.remote-base-url")
    BusinessProcessService csaRemoteBusinessProcessService(
            ObjectMapper objectMapper,
            @Value("${csa.bpm.remote-base-url}") String baseUrl) {
        return new RemoteBusinessProcessService(baseUrl, HttpClient.newHttpClient(), objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(BusinessProcessService.class)
    BusinessProcessService csaDisabledBusinessProcessService() {
        return new DisabledBusinessProcessService();
    }
}
