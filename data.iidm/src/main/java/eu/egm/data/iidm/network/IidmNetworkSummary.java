package eu.egm.data.iidm.network;

import com.powsybl.iidm.network.Network;
import eu.egm.data.iidm.common.IidmDiagnostic;
import java.util.List;

/**
 * Lightweight serializable view of a PowSyBl IIDM network.
 */
public record IidmNetworkSummary(
        String id,
        String importId,
        List<String> sourceFileIds,
        String businessDay,
        String businessTime,
        String timeFrame,
        String tsoName,
        String sourceFormat,
        int substationCount,
        int voltageLevelCount,
        int busCount,
        int lineCount,
        int generatorCount,
        int loadCount,
        int switchCount,
        int busbarSectionCount,
        List<IidmDiagnostic> diagnostics) {
    public IidmNetworkSummary {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public static IidmNetworkSummary from(IidmNetworkModel model) {
        Network network = model.network();
        return new IidmNetworkSummary(
                model.id(),
                model.importId(),
                model.sourceFileIds(),
                model.businessDay(),
                model.businessTime(),
                model.timeFrame(),
                model.tsoName(),
                network.getSourceFormat(),
                network.getSubstationCount(),
                network.getVoltageLevelCount(),
                (int) network.getBusView().getBusStream().count(),
                network.getLineCount(),
                network.getGeneratorCount(),
                network.getLoadCount(),
                network.getSwitchCount(),
                network.getBusbarSectionCount(),
                model.diagnostics());
    }
}
