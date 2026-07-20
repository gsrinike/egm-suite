package eu.egm.data.iidm.network;

import com.powsybl.iidm.network.Network;
import eu.egm.data.iidm.common.IidmDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Canonical IIDM transformation result backed by a real PowSyBl network.
 */
public record IidmNetworkModel(
        String id,
        String importId,
        List<String> sourceFileIds,
        String businessDay,
        String businessTime,
        String timeFrame,
        String tsoName,
        Network network,
        List<IidmDiagnostic> diagnostics) {
    public IidmNetworkModel {
        Objects.requireNonNull(network, "network is required");
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    /**
     * Creates a serializable summary from the PowSyBl network.
     */
    public IidmNetworkSummary summary() {
        return IidmNetworkSummary.from(this);
    }
}
