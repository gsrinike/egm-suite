package eu.egm.data.cgm.mapping;

import eu.egm.data.cgm.dto.cgmes.*;
import eu.egm.data.cgm.dto.iidm.*;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.commons.datasource.GenericReadOnlyDataSource;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class PowsyblCgmesEquipmentReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowsyblCgmesEquipmentReader.class);

    private final PowsyblIidmEquipmentReader iidmEquipmentReader;
    private final EquipmentProjectionReader fallbackReader;

    public PowsyblCgmesEquipmentReader() {
        this(new PowsyblIidmEquipmentReader(), new EquipmentProjectionReader());
    }

    public PowsyblCgmesEquipmentReader(PowsyblIidmEquipmentReader iidmEquipmentReader, EquipmentProjectionReader fallbackReader) {
        this.iidmEquipmentReader = iidmEquipmentReader;
        this.fallbackReader = fallbackReader;
    }

    public List<EquipmentView> read(String networkId, ImportMetadata metadata, InputStream inputStream) {
        try {
            byte[] payload = inputStream.readAllBytes();
            try {
                return readWithPowsybl(networkId, metadata, payload);
            } catch (RuntimeException exception) {
                LOGGER.debug("PowSyBl CGMES import failed for network {}; using equipment projection fallback", networkId, exception);
                return fallbackReader.read(networkId, metadata, payload);
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse CGMES payload", exception);
        }
    }

    private List<EquipmentView> readWithPowsybl(String networkId, ImportMetadata metadata, byte[] payload) {
        Path workingDirectory = null;
        try {
            workingDirectory = Files.createTempDirectory("egm-cgmes-import-");
            Path source = workingDirectory.resolve(CgmesPayloads.isZip(payload) ? "network.zip" : "network.xml");
            Files.write(source, payload);
            Network network = importNetwork(source);
            List<EquipmentView> equipment = iidmEquipmentReader.read(network).stream()
                    .map(iidmEquipment -> toEquipmentView(networkId, metadata, iidmEquipment))
                    .toList();
            if (equipment.isEmpty()) {
                throw new IllegalArgumentException("PowSyBl CGMES import did not produce IIDM equipment");
            }
            LOGGER.info("Imported {} IIDM equipment entries from CGMES payload for network {}", equipment.size(), networkId);
            return equipment;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to materialize CGMES payload for PowSyBl import", exception);
        } finally {
            deleteQuietly(workingDirectory);
        }
    }

    private Network importNetwork(Path source) {
        Properties parameters = new Properties();
        parameters.setProperty(CgmesImport.CREATE_BUSBAR_SECTION_FOR_EVERY_CONNECTIVITY_NODE, "false");
        parameters.setProperty(CgmesImport.CONVERT_SV_INJECTIONS, "true");
        return new CgmesImport().importData(new GenericReadOnlyDataSource(source), NetworkFactory.findDefault(), parameters, ReportNode.NO_OP);
    }

    private EquipmentView toEquipmentView(String networkId, ImportMetadata metadata, IidmEquipment equipment) {
        return new EquipmentView(
                equipment.id(),
                networkId,
                metadata,
                equipment.name(),
                toEquipmentType(equipment.type()),
                equipment.containerId(),
                equipment.nominalVoltage(),
                equipment.attributes());
    }

    private EquipmentType toEquipmentType(IidmEquipmentType type) {
        return switch (type) {
            case SUBSTATION -> EquipmentType.SUBSTATION;
            case VOLTAGE_LEVEL -> EquipmentType.VOLTAGE_LEVEL;
            case BUS -> EquipmentType.BUS;
            case LINE -> EquipmentType.LINE;
            case TWO_WINDINGS_TRANSFORMER -> EquipmentType.TRANSFORMER;
            case GENERATOR -> EquipmentType.GENERATOR;
            case LOAD -> EquipmentType.LOAD;
            case SHUNT_COMPENSATOR -> EquipmentType.SHUNT;
            case SWITCH -> EquipmentType.SWITCH;
            case STATE_VARIABLE -> EquipmentType.STATE_VARIABLE;
            case UNKNOWN -> EquipmentType.UNKNOWN;
        };
    }

    private void deleteQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    LOGGER.debug("Unable to delete temporary CGMES import path {}", path);
                }
            });
        } catch (IOException ignored) {
            LOGGER.debug("Unable to traverse temporary CGMES import directory {}", root);
        }
    }
}
