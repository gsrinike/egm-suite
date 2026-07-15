package eu.egm.srv.csa.services;

import com.utils.restservice.RestServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestServiceConfiguration.class)
public class CsaServicesApplication {
    public static void main(String[] args) {
        System.setProperty("module", "srv.csa.services");
        SpringApplication.run(CsaServicesApplication.class, args);
    }
}
