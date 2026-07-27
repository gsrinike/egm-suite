package eu.egm.srv.common.lfsa;

import com.infra.config.InfrastructureUtilityConfig;
import com.utils.restservice.RestServiceConfiguration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@EnableRabbit
@SpringBootApplication
@Import({RestServiceConfiguration.class, InfrastructureUtilityConfig.class})
public class CommonLfSaApplication {
    public static void main(String[] args) {
        System.setProperty("module", "srv.common.lfsa");
        SpringApplication.run(CommonLfSaApplication.class, args);
    }
}
