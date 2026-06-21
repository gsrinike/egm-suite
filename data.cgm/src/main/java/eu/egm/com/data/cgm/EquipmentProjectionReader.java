package eu.egm.com.data.cgm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class EquipmentProjectionReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(EquipmentProjectionReader.class);

    private final CgmProfileGraphLoader graphLoader;
    private final List<CgmTopologyMappingStrategy> strategies;

    public EquipmentProjectionReader() {
        this(new CgmProfileGraphLoader(), List.of(new NodeBreakerTopologyMappingStrategy(), new BusBranchTopologyMappingStrategy()));
    }

    public EquipmentProjectionReader(CgmProfileGraphLoader graphLoader, List<CgmTopologyMappingStrategy> strategies) {
        this.graphLoader = Objects.requireNonNull(graphLoader, "graphLoader is required");
        this.strategies = List.copyOf(Objects.requireNonNull(strategies, "strategies are required"));
    }

    public List<EquipmentView> read(String networkId, ImportMetadata metadata, byte[] payload) {
        CgmProfileGraph graph = graphLoader.load(payload);
        CgmTopologyMappingStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.supports(graph, metadata))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No CGM topology mapping strategy supports the uploaded profiles"));
        List<EquipmentView> equipment = strategy.project(networkId, metadata, graph);
        if (equipment.isEmpty()) {
            throw new IllegalArgumentException("CGM profiles did not contain supported equipment/state nodes");
        }
        LOGGER.info("Projected {} equipment/state entries from {} CGM graph nodes for network {} using {}",
                equipment.size(), graph.nodes().size(), networkId, strategy.getClass().getSimpleName());
        return equipment;
    }
}
