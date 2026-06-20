package eu.egm.com.auth;

import eu.egm.com.auth.config.KeycloakProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KeycloakProperties.class)
public class AuthApplication {

    public static void main(String[] args) {
        System.setProperty("module", "com.auth");
        SpringApplication.run(AuthApplication.class, args);
    }
}
