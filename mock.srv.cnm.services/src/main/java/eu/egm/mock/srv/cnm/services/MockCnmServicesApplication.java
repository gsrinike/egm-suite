package eu.egm.mock.srv.cnm.services;

import com.utils.restservice.RestServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestServiceConfiguration.class)
public class MockCnmServicesApplication {
    public static void main(String[] args) {
        System.setProperty("module", "mock.srv.cnm.services");
        SpringApplication.run(MockCnmServicesApplication.class, args);
    }
}
