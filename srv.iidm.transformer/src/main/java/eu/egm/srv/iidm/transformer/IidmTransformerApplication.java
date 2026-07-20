package eu.egm.srv.iidm.transformer;

import com.infra.config.InfrastructureUtilityConfig;
import com.utils.restservice.RestServiceConfiguration;
import eu.egm.mapping.JacksonJsonMappingService;
import eu.egm.mapping.JsonMappingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({InfrastructureUtilityConfig.class, RestServiceConfiguration.class})
public class IidmTransformerApplication {
    public static void main(String[] args) {
        System.setProperty("module", "srv.iidm.transformer");
        SpringApplication.run(IidmTransformerApplication.class, args);
    }

    @Bean
    JsonMappingService jsonMappingService() {
        return new JacksonJsonMappingService();
    }
}
