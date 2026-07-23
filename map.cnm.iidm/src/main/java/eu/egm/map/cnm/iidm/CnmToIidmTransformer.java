package eu.egm.map.cnm.iidm;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import eu.egm.data.cnm.common.GridTopologyObject;
import eu.egm.data.cnm.common.GridTopologyRelation;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.iidm.common.IidmDiagnostic;
import eu.egm.data.iidm.network.IidmNetworkModel;
import eu.egm.mapping.MappingConfiguration;
import eu.egm.mapping.MappingService;
import eu.egm.mapping.transformer.Transformer;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Converts parsed CNM profile payloads into a real PowSyBl IIDM network.
 */
public class CnmToIidmTransformer implements Transformer<IidmNetworkModel> {
    private final MappingService mappingService;
    private final MappingConfiguration mappingConfiguration;
    private final CnmToIidmMappingConfiguration cnmConfiguration;

    public CnmToIidmTransformer(MappingService mappingService, MappingConfiguration mappingConfiguration) {
        this.mappingService = mappingService;
        this.mappingConfiguration = mappingConfiguration;
        this.cnmConfiguration = mappingConfiguration instanceof CnmToIidmMappingConfiguration configuration
                ? configuration
                : new CnmToIidmMappingConfiguration();
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
     * Transforms one parsed profile into a PowSyBl IIDM network.
     */
    public IidmNetworkModel transform(
            ProfilePayload<?> payload,
            String importId,
            String businessDay,
            String businessTime,
            String timeFrame,
            String tsoName) {
        if (payload == null) {
            throw new IllegalArgumentException("Profile payload is required");
        }

        String networkId = networkId(importId, payload.fileId());
        Network network = Network.create(networkId, "CGMES");
        network.setCaseDate(caseDate(businessDay, businessTime));
        setProperty(network, "egm.importId", importId);
        setProperty(network, "egm.fileId", payload.fileId());
        setProperty(network, "egm.objectId", payload.objectId());
        setProperty(network, "egm.profileType", payload.profileType());
        setProperty(network, "egm.timeFrame", timeFrame);
        setProperty(network, "egm.tsoName", tsoName);

        CnmLookup lookup = new CnmLookup(payload);
        List<IidmDiagnostic> diagnostics = new ArrayList<>();

        Map<String, Substation> substations = createSubstations(network, payload, tsoName);
        Map<String, VoltageLevel> voltageLevels = createVoltageLevels(network, payload, lookup, substations);
        Map<String, String> busByNodeId = createBuses(payload, lookup, voltageLevels, diagnostics);

        createEquipment(network, payload, lookup, busByNodeId, diagnostics);
        applySolvedState(payload, lookup, busByNodeId, network, diagnostics);

        payload.warnings().forEach(warning -> diagnostics.add(new IidmDiagnostic("WARN", "CNM_WARNING", warning, payload.fileId())));
        return new IidmNetworkModel(
                networkId,
                importId,
                List.of(payload.fileId()),
                businessDay,
                businessTime,
                timeFrame,
                tsoName,
                network,
                diagnostics);
    }

    private Map<String, Substation> createSubstations(Network network, ProfilePayload<?> payload, String tsoName) {
        Map<String, Substation> substations = new LinkedHashMap<>();
        for (GridTopologyObject object : payload.topologyObjects()) {
            if (normalized(object.objectType()).contains("SUBSTATION")) {
                Substation substation = network.newSubstation()
                        .setId(id(object.mRID()))
                        .setName(name(object))
                        .setTso(tsoName)
                        .add();
                copyAttributes(object, substation);
                substations.put(object.mRID(), substation);
            }
        }
        if (substations.isEmpty()) {
            Substation substation = network.newSubstation()
                    .setId(cnmConfiguration.defaultSubstationId())
                    .setName(cnmConfiguration.defaultSubstationName())
                    .setTso(tsoName)
                    .add();
            substations.put(substation.getId(), substation);
        }
        return substations;
    }

    private Map<String, VoltageLevel> createVoltageLevels(
            Network network,
            ProfilePayload<?> payload,
            CnmLookup lookup,
            Map<String, Substation> substations) {
        Map<String, VoltageLevel> voltageLevels = new LinkedHashMap<>();
        Substation defaultSubstation = substations.values().iterator().next();
        for (GridTopologyObject object : payload.topologyObjects()) {
            if (normalized(object.objectType()).contains("VOLTAGELEVEL")) {
                String substationId = lookup.firstTarget(object.mRID(), "Substation").orElse("");
                Substation substation = substations.getOrDefault(substationId, defaultSubstation);
                VoltageLevel voltageLevel = substation.newVoltageLevel()
                        .setId(id(object.mRID()))
                        .setName(name(object))
                        .setTopologyKind(TopologyKind.BUS_BREAKER)
                        .setNominalV(number(object.attributes().get("nominalVoltage"), cnmConfiguration.nominalVoltage()))
                        .add();
                copyAttributes(object, voltageLevel);
                voltageLevels.put(object.mRID(), voltageLevel);
            }
        }
        if (voltageLevels.isEmpty()) {
            VoltageLevel voltageLevel = defaultSubstation.newVoltageLevel()
                    .setId(cnmConfiguration.defaultVoltageLevelId())
                    .setName(cnmConfiguration.defaultVoltageLevelName())
                    .setTopologyKind(TopologyKind.BUS_BREAKER)
                    .setNominalV(cnmConfiguration.nominalVoltage())
                    .add();
            voltageLevels.put(voltageLevel.getId(), voltageLevel);
        }
        return voltageLevels;
    }

    private Map<String, String> createBuses(
            ProfilePayload<?> payload,
            CnmLookup lookup,
            Map<String, VoltageLevel> voltageLevels,
            List<IidmDiagnostic> diagnostics) {
        Map<String, String> busByNodeId = new LinkedHashMap<>();
        VoltageLevel defaultVoltageLevel = voltageLevels.values().iterator().next();
        for (GridTopologyObject object : payload.topologyObjects()) {
            String type = normalized(object.objectType());
            if (type.contains("TOPOLOGICALNODE") || type.contains("CONNECTIVITYNODE")) {
                String voltageLevelId = lookup.firstTarget(object.mRID(), "VoltageLevel").orElse("");
                VoltageLevel voltageLevel = voltageLevels.getOrDefault(voltageLevelId, defaultVoltageLevel);
                Bus bus = voltageLevel.getBusBreakerView().newBus()
                        .setId(id(object.mRID()))
                        .setName(name(object))
                        .add();
                copyAttributes(object, bus);
                setIfPresent(object.attributes().get("v"), bus::setV);
                setIfPresent(object.attributes().get("angle"), bus::setAngle);
                busByNodeId.put(object.mRID(), bus.getId());
            }
        }
        if (busByNodeId.isEmpty()) {
            Bus bus = defaultVoltageLevel.getBusBreakerView().newBus()
                    .setId(cnmConfiguration.defaultBusId())
                    .setName(cnmConfiguration.defaultBusName())
                    .add();
            busByNodeId.put(bus.getId(), bus.getId());
            diagnostics.add(new IidmDiagnostic("WARN", "IIDM_DEFAULT_BUS", "Created a default bus because no topology node was extracted", ""));
        }
        return busByNodeId;
    }

    private void createEquipment(
            Network network,
            ProfilePayload<?> payload,
            CnmLookup lookup,
            Map<String, String> busByNodeId,
            List<IidmDiagnostic> diagnostics) {
        for (GridTopologyObject object : payload.topologyObjects()) {
            String type = normalized(object.objectType());
            if (isContainerOrTopology(type) || type.contains("SV")) {
                continue;
            }
            if (type.contains("LINE") || type.contains("ACLINESEGMENT")) {
                createLine(network, object, lookup, busByNodeId, diagnostics);
            } else if (type.contains("GENERATOR")) {
                createGenerator(network, object, lookup, busByNodeId, diagnostics);
            } else if (type.contains("LOAD")) {
                createLoad(network, object, lookup, busByNodeId, diagnostics);
            } else {
                diagnostics.add(new IidmDiagnostic("INFO", "IIDM_UNMAPPED_EQUIPMENT",
                        "Retained CNM equipment as network property: " + object.objectType(), object.mRID()));
                setProperty(network, "egm.unmapped." + safePropertyKey(object.mRID()), object.objectType());
            }
        }
    }

    private void createLine(
            Network network,
            GridTopologyObject object,
            CnmLookup lookup,
            Map<String, String> busByNodeId,
            List<IidmDiagnostic> diagnostics) {
        List<String> busIds = terminalBusIds(object.mRID(), lookup, busByNodeId);
        if (busIds.size() < 2) {
            diagnostics.add(new IidmDiagnostic("WARN", "IIDM_LINE_SKIPPED",
                    "Line requires two terminal bus references", object.mRID()));
            return;
        }
        VoltageLevel voltageLevel1 = voltageLevel(network, busIds.get(0));
        VoltageLevel voltageLevel2 = voltageLevel(network, busIds.get(1));
        try {
            Line line = network.newLine()
                    .setId(id(object.mRID()))
                    .setName(name(object))
                    .setVoltageLevel1(voltageLevel1.getId())
                    .setBus1(busIds.get(0))
                    .setConnectableBus1(busIds.get(0))
                    .setVoltageLevel2(voltageLevel2.getId())
                    .setBus2(busIds.get(1))
                    .setConnectableBus2(busIds.get(1))
                    .setR(number(object.attributes().get("r"), 0.0))
                    .setX(number(object.attributes().get("x"), cnmConfiguration.defaultLineX()))
                    .setG1(number(object.attributes().get("g1"), 0.0))
                    .setB1(number(object.attributes().get("b1"), 0.0))
                    .setG2(number(object.attributes().get("g2"), 0.0))
                    .setB2(number(object.attributes().get("b2"), 0.0))
                    .add();
            copyAttributes(object, line);
        } catch (RuntimeException exception) {
            diagnostics.add(new IidmDiagnostic("WARN", "IIDM_LINE_FAILED", exception.getMessage(), object.mRID()));
        }
    }

    private void createGenerator(
            Network network,
            GridTopologyObject object,
            CnmLookup lookup,
            Map<String, String> busByNodeId,
            List<IidmDiagnostic> diagnostics) {
        Optional<String> busId = terminalBusIds(object.mRID(), lookup, busByNodeId).stream().findFirst()
                .or(() -> busByNodeId.values().stream().findFirst());
        if (busId.isEmpty()) {
            diagnostics.add(new IidmDiagnostic("WARN", "IIDM_GENERATOR_SKIPPED", "Generator has no bus reference", object.mRID()));
            return;
        }
        VoltageLevel voltageLevel = voltageLevel(network, busId.get());
        try {
            Generator generator = voltageLevel.newGenerator()
                    .setId(id(object.mRID()))
                    .setName(name(object))
                    .setBus(busId.get())
                    .setConnectableBus(busId.get())
                    .setEnergySource(EnergySource.OTHER)
                    .setMinP(number(object.attributes().get("minP"), 0.0))
                    .setMaxP(number(object.attributes().get("maxP"), 9999.0))
                    .setTargetP(number(object.attributes().get("targetP"), 0.0))
                    .setTargetQ(number(object.attributes().get("targetQ"), 0.0))
                    .setVoltageRegulatorOn(false)
                    .add();
            copyAttributes(object, generator);
        } catch (RuntimeException exception) {
            diagnostics.add(new IidmDiagnostic("WARN", "IIDM_GENERATOR_FAILED", exception.getMessage(), object.mRID()));
        }
    }

    private void createLoad(
            Network network,
            GridTopologyObject object,
            CnmLookup lookup,
            Map<String, String> busByNodeId,
            List<IidmDiagnostic> diagnostics) {
        Optional<String> busId = terminalBusIds(object.mRID(), lookup, busByNodeId).stream().findFirst()
                .or(() -> busByNodeId.values().stream().findFirst());
        if (busId.isEmpty()) {
            diagnostics.add(new IidmDiagnostic("WARN", "IIDM_LOAD_SKIPPED", "Load has no bus reference", object.mRID()));
            return;
        }
        VoltageLevel voltageLevel = voltageLevel(network, busId.get());
        try {
            Load load = voltageLevel.newLoad()
                    .setId(id(object.mRID()))
                    .setName(name(object))
                    .setBus(busId.get())
                    .setConnectableBus(busId.get())
                    .setLoadType(LoadType.UNDEFINED)
                    .setP0(number(object.attributes().get("p0"), number(object.attributes().get("p"), 0.0)))
                    .setQ0(number(object.attributes().get("q0"), number(object.attributes().get("q"), 0.0)))
                    .add();
            copyAttributes(object, load);
        } catch (RuntimeException exception) {
            diagnostics.add(new IidmDiagnostic("WARN", "IIDM_LOAD_FAILED", exception.getMessage(), object.mRID()));
        }
    }

    private void applySolvedState(
            ProfilePayload<?> payload,
            CnmLookup lookup,
            Map<String, String> busByNodeId,
            Network network,
            List<IidmDiagnostic> diagnostics) {
        for (GridTopologyObject object : payload.topologyObjects()) {
            String type = normalized(object.objectType());
            if (!type.contains("SV")) {
                continue;
            }
            Optional<String> nodeId = lookup.firstTarget(object.mRID(), "TopologicalNode")
                    .or(() -> lookup.firstTarget(object.mRID(), "ConnectivityNode"));
            if (nodeId.isPresent()) {
                String busId = busByNodeId.get(nodeId.get());
                if (busId != null) {
                    Bus bus = network.getBusBreakerView().getBus(busId);
                    if (bus != null) {
                        setIfPresent(object.attributes().get("v"), bus::setV);
                        setIfPresent(object.attributes().get("angle"), bus::setAngle);
                    }
                }
            } else {
                diagnostics.add(new IidmDiagnostic("INFO", "IIDM_SV_UNRESOLVED",
                        "State variable has no topology target", object.mRID()));
            }
        }
    }

    private List<String> terminalBusIds(String equipmentId, CnmLookup lookup, Map<String, String> busByNodeId) {
        Set<String> busIds = new LinkedHashSet<>();
        for (String terminalId : lookup.terminalsForEquipment(equipmentId)) {
            lookup.firstTarget(terminalId, "TopologicalNode")
                    .or(() -> lookup.firstTarget(terminalId, "ConnectivityNode"))
                    .map(busByNodeId::get)
                    .ifPresent(busIds::add);
        }
        lookup.firstTarget(equipmentId, "TopologicalNode")
                .or(() -> lookup.firstTarget(equipmentId, "ConnectivityNode"))
                .map(busByNodeId::get)
                .ifPresent(busIds::add);
        return busIds.stream().sorted().toList();
    }

    private VoltageLevel voltageLevel(Network network, String busId) {
        Bus bus = network.getBusBreakerView().getBus(busId);
        if (bus == null) {
            throw new IllegalArgumentException("Unknown bus: " + busId);
        }
        return bus.getVoltageLevel();
    }

    private boolean isContainerOrTopology(String normalizedType) {
        return normalizedType.contains("SUBSTATION")
                || normalizedType.contains("VOLTAGELEVEL")
                || normalizedType.contains("TOPOLOGICALNODE")
                || normalizedType.contains("CONNECTIVITYNODE")
                || normalizedType.contains("TERMINAL");
    }

    private void copyAttributes(GridTopologyObject object, Identifiable<?> identifiable) {
        setProperty(identifiable, "egm.cnm.mRID", object.mRID());
        setProperty(identifiable, "egm.cnm.profileType", object.profileType());
        setProperty(identifiable, "egm.cnm.objectType", object.objectType());
        object.attributes().forEach((key, value) -> setProperty(identifiable, "egm.cnm.attr." + safePropertyKey(key), stringValue(value)));
    }

    private void setProperty(Identifiable<?> identifiable, String key, Object value) {
        if (value != null) {
            identifiable.setProperty(key, stringValue(value));
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void setIfPresent(Object value, DoubleSetter setter) {
        Double number = numberOrNull(value);
        if (number != null) {
            setter.set(number);
        }
    }

    private ZonedDateTime caseDate(String businessDay, String businessTime) {
        try {
            String time = businessTime == null || businessTime.isBlank() ? "00:00" : businessTime;
            return ZonedDateTime.parse(businessDay + "T" + time + ":00Z");
        } catch (RuntimeException exception) {
            return ZonedDateTime.now(ZoneOffset.UTC);
        }
    }

    private String id(String value) {
        return value == null || value.isBlank() ? "EGM_" + Math.abs(System.nanoTime()) : value;
    }

    private String name(GridTopologyObject object) {
        return object.name() == null || object.name().isBlank() ? object.mRID() : object.name();
    }

    private String networkId(String importId, String fileId) {
        return importId + ":" + fileId;
    }

    private double number(Object value, double defaultValue) {
        Double parsed = numberOrNull(value);
        return parsed == null ? defaultValue : parsed;
    }

    private Double numberOrNull(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalized(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replace(".", "");
    }

    private String safePropertyKey(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    @FunctionalInterface
    private interface DoubleSetter {
        void set(double value);
    }

    private static final class CnmLookup {
        private final Map<String, List<GridTopologyRelation>> bySource = new LinkedHashMap<>();
        private final Map<String, List<GridTopologyRelation>> byTarget = new LinkedHashMap<>();

        private CnmLookup(ProfilePayload<?> payload) {
            for (GridTopologyRelation relation : payload.topologyRelations()) {
                bySource.computeIfAbsent(relation.sourceMRID(), ignored -> new ArrayList<>()).add(relation);
                byTarget.computeIfAbsent(relation.targetMRID(), ignored -> new ArrayList<>()).add(relation);
            }
        }

        private Optional<String> firstTarget(String sourceId, String relationToken) {
            String token = normalize(relationToken);
            return bySource.getOrDefault(sourceId, List.of()).stream()
                    .filter(relation -> token.isBlank() || normalize(relation.relationType()).contains(token))
                    .map(GridTopologyRelation::targetMRID)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst();
        }

        private List<String> terminalsForEquipment(String equipmentId) {
            return byTarget.getOrDefault(equipmentId, List.of()).stream()
                    .filter(relation -> normalize(relation.relationType()).contains("TERMINAL")
                            || normalize(relation.relationType()).contains("CONDUCTINGEQUIPMENT"))
                    .map(GridTopologyRelation::sourceMRID)
                    .filter(value -> value != null && !value.isBlank())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }

        private String normalize(String value) {
            return value == null ? "" : value.toUpperCase(Locale.ROOT).replace(".", "");
        }
    }
}
