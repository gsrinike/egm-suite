package eu.egm.srv.cgm.importer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"eu.egm", "com.infra"})
public class CgmImporterApplication {
    public static void main(String[] args) {
        System.setProperty("module", "srv.cgm.importer");
        SpringApplication.run(CgmImporterApplication.class, args);
    }
}
