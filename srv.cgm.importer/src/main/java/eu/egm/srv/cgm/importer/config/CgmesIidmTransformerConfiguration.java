package eu.egm.srv.cgm.importer.config;

import eu.egm.map.cgmes.iidm.DefaultCgmesIidmTransformerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CgmesIidmTransformerConfiguration {
    @Bean
    public DefaultCgmesIidmTransformerFactory cgmesIidmTransformerFactory() {
        return new DefaultCgmesIidmTransformerFactory();
    }
}
