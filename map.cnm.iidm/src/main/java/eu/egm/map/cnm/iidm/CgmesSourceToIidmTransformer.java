package eu.egm.map.cnm.iidm;

import com.powsybl.commons.datasource.DirectoryDataSource;
import com.powsybl.iidm.network.Network;
import eu.egm.data.iidm.common.IidmDiagnostic;
import eu.egm.data.iidm.network.IidmNetworkModel;
import eu.egm.mapping.MappingConfiguration;
import eu.egm.mapping.MappingService;
import eu.egm.mapping.transformer.Transformer;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Converts raw CGMES source files directly to PowSyBl IIDM using PowSyBl's
 * native CIM-CGMES importer.
 */
public class CgmesSourceToIidmTransformer implements Transformer<IidmNetworkModel> {
    private final MappingService mappingService;
    private final MappingConfiguration mappingConfiguration;
    private final CgmesSourceToIidmMappingConfiguration cgmesConfiguration;

    public CgmesSourceToIidmTransformer(MappingService mappingService, MappingConfiguration mappingConfiguration) {
        this.mappingService = mappingService;
        this.mappingConfiguration = mappingConfiguration;
        this.cgmesConfiguration = mappingConfiguration instanceof CgmesSourceToIidmMappingConfiguration configuration
                ? configuration
                : new CgmesSourceToIidmMappingConfiguration(Map.of());
    }

    @Override
    public MappingService mappingService() {
        return mappingService;
    }

    @Override
    public MappingConfiguration mappingConfiguration() {
        return mappingConfiguration;
    }

    /**
     * Imports a staged CGMES directory or archive into an IIDM network.
     */
    public IidmNetworkModel transform(
            Path sourcePath,
            String networkId,
            String importId,
            List<String> sourceFileIds,
            String businessDay,
            String businessTime,
            String timeFrame,
            String tsoName,
            Map<String, String> requestProperties) {
        if (sourcePath == null) {
            throw new IllegalArgumentException("CGMES source path is required");
        }
        Properties properties = importProperties(requestProperties);
        Network network = Network.read(new DirectoryDataSource(sourcePath, "", "", true, null), properties);
        network.setCaseDate(caseDate(businessDay, businessTime));
        setProperty(network, "egm.importId", importId);
        setProperty(network, "egm.source", "CGMES_SOURCE");
        setProperty(network, "egm.transformId", networkId);
        setProperty(network, "egm.timeFrame", timeFrame);
        setProperty(network, "egm.tsoName", tsoName);
        setProperty(network, "egm.sourceFileIds", String.join(",", sourceFileIds == null ? List.of() : sourceFileIds));
        return new IidmNetworkModel(
                networkId,
                importId,
                sourceFileIds,
                businessDay,
                businessTime,
                timeFrame,
                tsoName,
                network,
                List.of(new IidmDiagnostic("INFO", "POWSYBL_CGMES_IMPORT", "Imported directly from CGMES source files", networkId)));
    }

    private Properties importProperties(Map<String, String> requestProperties) {
        Properties properties = new Properties();
        cgmesConfiguration.importProperties().forEach(properties::put);
        if (requestProperties != null) {
            requestProperties.forEach(properties::put);
        }
        return properties;
    }

    private ZonedDateTime caseDate(String businessDay, String businessTime) {
        try {
            LocalDate date = businessDay == null || businessDay.isBlank() ? LocalDate.now(ZoneOffset.UTC) : LocalDate.parse(businessDay);
            LocalTime time = businessTime == null || businessTime.isBlank() ? LocalTime.MIDNIGHT : LocalTime.parse(businessTime);
            return ZonedDateTime.of(date, time, ZoneOffset.UTC);
        } catch (Exception ignored) {
            return ZonedDateTime.now(ZoneOffset.UTC);
        }
    }

    private void setProperty(Network network, String name, String value) {
        if (network != null && value != null && !value.isBlank()) {
            network.setProperty(name, value);
        }
    }
}
