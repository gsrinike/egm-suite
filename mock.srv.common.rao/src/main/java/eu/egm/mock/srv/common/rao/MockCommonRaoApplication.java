package eu.egm.mock.srv.common.rao;

import com.utils.restservice.RestServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestServiceConfiguration.class)
public class MockCommonRaoApplication {
    public static void main(String[] args) {
        System.setProperty("module", "mock.srv.common.rao");
        SpringApplication.run(MockCommonRaoApplication.class, args);
    }
}
