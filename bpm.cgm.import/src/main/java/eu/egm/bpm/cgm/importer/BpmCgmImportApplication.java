package eu.egm.bpm.cgm.importer;

import eu.egm.bpm.cgm.importer.config.CgmImportBpmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"eu.egm", "com.infra"})
@EnableConfigurationProperties(CgmImportBpmProperties.class)
public class BpmCgmImportApplication {
    public static void main(String[] args) {
        System.setProperty("module", "bpm.cgm.import");
        SpringApplication.run(BpmCgmImportApplication.class, args);
    }
}
