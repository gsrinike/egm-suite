package eu.egm.srv.cnm.services;

import com.infra.config.InfrastructureUtilityConfig;
import com.utils.restservice.RestServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({InfrastructureUtilityConfig.class, RestServiceConfiguration.class})
public class CnmServicesApplication {
    public static void main(String[] args) {
        System.setProperty("module", "srv.cnm.services");
        SpringApplication.run(CnmServicesApplication.class, args);
    }
}
