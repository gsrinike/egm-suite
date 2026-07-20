package eu.egm.data.iidm.network;

import com.powsybl.iidm.network.Network;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * XIIDM serialization helpers for PowSyBl networks.
 */
public final class IidmNetworkXiidm {
    public static final String FORMAT = "XIIDM";

    private IidmNetworkXiidm() {
    }

    public static String write(Network network) {
        try {
            Path file = Files.createTempFile("egm-iidm-", ".xiidm");
            try {
                network.write(FORMAT, new Properties(), file);
                return Files.readString(file, StandardCharsets.UTF_8);
            } finally {
                Files.deleteIfExists(file);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize PowSyBl network as XIIDM", exception);
        }
    }

    public static Network read(String xiidm) {
        try {
            Path file = Files.createTempFile("egm-iidm-", ".xiidm");
            try {
                Files.writeString(file, xiidm, StandardCharsets.UTF_8);
                return Network.read(file);
            } finally {
                Files.deleteIfExists(file);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read PowSyBl XIIDM network", exception);
        }
    }
}
