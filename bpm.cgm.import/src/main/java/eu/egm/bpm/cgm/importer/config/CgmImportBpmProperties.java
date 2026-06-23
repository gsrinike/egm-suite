package eu.egm.bpm.cgm.importer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bpm.cgm.import")
public record CgmImportBpmProperties(String serviceBaseUrl) {
}
