package eu.egm.srv.cnm.services;

import com.infra.config.InfrastructureUtilityConfig;
import com.utils.restservice.RestServiceConfiguration;
import eu.egm.srv.cnm.services.domain.CnmImportDocument;
import eu.egm.srv.cnm.services.domain.CnmImportDocument.CnmImportFileDocument;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({InfrastructureUtilityConfig.class, RestServiceConfiguration.class})
public class CnmServicesApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(CnmServicesApplication.class);

    public static void main(String[] args) {
        System.setProperty("module", "srv.cnm.services");
        SpringApplication.run(CnmServicesApplication.class, args);
    }

    @Bean
    SmartInitializingSingleton cnmDocumentSchemaValidator() {
        return () -> {
            validateTimestampComponent(CnmImportDocument.class, "createdAt");
            validateTimestampComponent(CnmImportFileDocument.class, "uploadedAt");
        };
    }

    private void validateTimestampComponent(Class<?> documentType, String componentName) {
        Class<?> componentType = Arrays.stream(documentType.getRecordComponents())
                .filter(component -> component.getName().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing CNM document component " + documentType.getName() + "." + componentName))
                .getType();
        LOGGER.info("CNM document schema {}.{} uses {}", documentType.getSimpleName(), componentName, componentType);
        if (componentType != Object.class) {
            throw new IllegalStateException(
                    "Stale CNM document class loaded: " + documentType.getName() + "." + componentName
                            + " must use Object but uses " + componentType.getName());
        }
    }
}
