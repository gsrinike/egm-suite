package eu.egm.srv.common.lfsa;

import com.utils.restservice.RestServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestServiceConfiguration.class)
public class CommonLfsaApplication {
    public static void main(String[] args) {
        System.setProperty("module", "srv.common.lfsa");
        SpringApplication.run(CommonLfsaApplication.class, args);
    }
}
