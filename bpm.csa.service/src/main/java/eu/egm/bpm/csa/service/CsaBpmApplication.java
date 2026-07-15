package eu.egm.bpm.csa.service;

import com.infra.config.CamundaBpmInfrastructureConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(CamundaBpmInfrastructureConfig.class)
public class CsaBpmApplication {
    public static void main(String[] args) {
        System.setProperty("module", "bpm.csa.service");
        SpringApplication.run(CsaBpmApplication.class, args);
    }
}
