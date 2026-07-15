package eu.egm.mock.srv.csa.services;

import com.utils.restservice.RestServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestServiceConfiguration.class)
public class MockCsaServicesApplication {
    public static void main(String[] args) {
        System.setProperty("module", "mock.srv.csa.services");
        SpringApplication.run(MockCsaServicesApplication.class, args);
    }
}
