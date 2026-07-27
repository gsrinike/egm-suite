package eu.egm.mock.srv.common.lfsa;

import com.utils.restservice.RestServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestServiceConfiguration.class)
public class MockCommonLfSaApplication {
    public static void main(String[] args) {
        System.setProperty("module", "mock.srv.common.lfsa");
        SpringApplication.run(MockCommonLfSaApplication.class, args);
    }
}
