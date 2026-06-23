package eu.egm.bpm.cgm.importer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CgmImportBpmConfig {
    @Bean
    RestClient cgmImportRestClient(CgmImportBpmProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.serviceBaseUrl())
                .build();
    }
}
