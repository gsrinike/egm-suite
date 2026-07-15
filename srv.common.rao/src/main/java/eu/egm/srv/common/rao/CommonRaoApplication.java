package eu.egm.srv.common.rao;

import com.utils.restservice.RestServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestServiceConfiguration.class)
public class CommonRaoApplication {
    public static void main(String[] args) {
        System.setProperty("module", "srv.common.rao");
        SpringApplication.run(CommonRaoApplication.class, args);
    }
}
