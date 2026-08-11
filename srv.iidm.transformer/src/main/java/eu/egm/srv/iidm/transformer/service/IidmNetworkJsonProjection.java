package eu.egm.srv.iidm.transformer.service;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.commons.extensions.Extension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Builds a stable JSON-friendly projection from a PowSyBl IIDM network.
 *
 * <p>The projection is optimized for GUI table rendering and search. XIIDM
 * remains the canonical round-trip representation, while this projection avoids
 * reparsing XML when users browse individual network element tables.</p>
 */
public class IidmNetworkJsonProjection {
    public static final String TYPE = "egm.iidm.table-projection.v1";

    public Map<String, List<Map<String, Object>>> project(Network network) {
        Map<String, List<Map<String, Object>>> projection = new LinkedHashMap<>();
        projection.put("substations", stream(network.getSubstations()).stream().map(this::substationRow).toList());
        projection.put("voltage-levels", stream(network.getVoltageLevels()).stream().map(this::voltageLevelRow).toList());
        projection.put("buses", stream(network.getBusBreakerView().getBuses()).stream().map(this::busRow).toList());
        projection.put("lines", stream(network.getLines()).stream().map(this::lineRow).toList());
        projection.put("generators", stream(network.getGenerators()).stream().map(this::generatorRow).toList());
        projection.put("loads", stream(network.getLoads()).stream().map(this::loadRow).toList());
        projection.put("switches", stream(network.getSwitches()).stream().map(this::switchRow).toList());
        projection.put("busbar-sections", stream(network.getBusbarSections()).stream().map(this::busbarSectionRow).toList());
        projection.put("substation-positions", stream(network.getSubstations()).stream()
                .flatMap(substation -> extensionPositionRows(substation).stream())
                .toList());
        projection.put("line-positions", stream(network.getLines()).stream()
                .flatMap(line -> extensionPositionRows(line).stream())
                .toList());
        projection.put("iidm-positions", allPositionRows(network));
        return projection;
    }

    private Map<String, Object> substationRow(Substation substation) {
        return identifiableRow(substation,
                "country", substation.getCountry().map(Enum::name).orElse(""),
                "tso", substation.getTso());
    }

    private Map<String, Object> voltageLevelRow(VoltageLevel voltageLevel) {
        return identifiableRow(voltageLevel,
                "substationId", voltageLevel.getSubstation().map(Identifiable::getId).orElse(""),
                "nominalV", voltageLevel.getNominalV(),
                "lowVoltageLimit", voltageLevel.getLowVoltageLimit(),
                "highVoltageLimit", voltageLevel.getHighVoltageLimit(),
                "topologyKind", voltageLevel.getTopologyKind().name());
    }

    private Map<String, Object> busRow(Bus bus) {
        return identifiableRow(bus,
                "voltageLevelId", bus.getVoltageLevel().getId(),
                "v", bus.getV(),
                "angle", bus.getAngle(),
                "p", bus.getP(),
                "q", bus.getQ(),
                "connectedTerminalCount", bus.getConnectedTerminalCount());
    }

    private Map<String, Object> lineRow(Line line) {
        return identifiableRow(line,
                "voltageLevel1", voltageLevelId(line.getTerminal1()),
                "voltageLevel2", voltageLevelId(line.getTerminal2()),
                "r", line.getR(),
                "x", line.getX(),
                "g1", line.getG1(),
                "g2", line.getG2(),
                "b1", line.getB1(),
                "b2", line.getB2());
    }

    private Map<String, Object> generatorRow(Generator generator) {
        return identifiableRow(generator,
                "voltageLevelId", voltageLevelId(generator.getTerminal()),
                "energySource", generator.getEnergySource().name(),
                "minP", generator.getMinP(),
                "maxP", generator.getMaxP(),
                "targetP", generator.getTargetP(),
                "targetQ", generator.getTargetQ(),
                "targetV", generator.getTargetV(),
                "voltageRegulatorOn", generator.isVoltageRegulatorOn());
    }

    private Map<String, Object> loadRow(Load load) {
        return identifiableRow(load,
                "voltageLevelId", voltageLevelId(load.getTerminal()),
                "loadType", load.getLoadType().name(),
                "p0", load.getP0(),
                "q0", load.getQ0());
    }

    private Map<String, Object> switchRow(Switch networkSwitch) {
        return identifiableRow(networkSwitch,
                "voltageLevelId", networkSwitch.getVoltageLevel().getId(),
                "kind", networkSwitch.getKind().name(),
                "open", networkSwitch.isOpen(),
                "retained", networkSwitch.isRetained());
    }

    private Map<String, Object> busbarSectionRow(BusbarSection busbarSection) {
        return identifiableRow(busbarSection,
                "voltageLevelId", voltageLevelId(busbarSection.getTerminal()),
                "v", busbarSection.getV(),
                "angle", busbarSection.getAngle());
    }

    private List<Map<String, Object>> extensionPositionRows(Identifiable<?> identifiable) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Extension<?> extension : identifiable.getExtensions()) {
            if (!extension.getClass().getSimpleName().toLowerCase().contains("position")) {
                continue;
            }
            Object points = invoke(extension, "getPoints");
            Collection<?> collection;
            if (points instanceof Collection<?> pointCollection) {
                collection = pointCollection;
            } else {
                points = invoke(extension, "getCoordinates");
                collection = points instanceof Collection<?> coordinates ? coordinates : List.of();
            }
            if (collection.isEmpty()) {
                rows.add(positionRow(identifiable, extension, null, 1));
            } else {
                int sequence = 1;
                for (Object point : collection) {
                    rows.add(positionRow(identifiable, extension, point, sequence++));
                }
            }
        }
        return rows;
    }

    private List<Map<String, Object>> allPositionRows(Network network) {
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Identifiable<?> identifiable : identifiableElements(network)) {
            for (Map<String, Object> row : extensionPositionRows(identifiable)) {
                String rowId = String.valueOf(row.getOrDefault("rowId", ""));
                if (seen.add(rowId)) {
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private List<Identifiable<?>> identifiableElements(Network network) {
        List<Identifiable<?>> elements = new ArrayList<>();
        addElements(elements, network, "getSubstations");
        addElements(elements, network, "getVoltageLevels");
        addElements(elements, network, "getLines");
        addElements(elements, network, "getTwoWindingsTransformers");
        addElements(elements, network, "getThreeWindingsTransformers");
        addElements(elements, network, "getGenerators");
        addElements(elements, network, "getLoads");
        addElements(elements, network, "getBatteries");
        addElements(elements, network, "getShuntCompensators");
        addElements(elements, network, "getStaticVarCompensators");
        addElements(elements, network, "getDanglingLines");
        addElements(elements, network, "getHvdcLines");
        addElements(elements, network, "getSwitches");
        addElements(elements, network, "getBusbarSections");
        return elements;
    }

    private void addElements(List<Identifiable<?>> elements, Network network, String methodName) {
        Object result = invoke(network, methodName);
        if (!(result instanceof Iterable<?> iterable)) {
            return;
        }
        for (Object value : iterable) {
            if (value instanceof Identifiable<?> identifiable) {
                elements.add(identifiable);
            }
        }
    }

    private Map<String, Object> positionRow(Identifiable<?> identifiable, Extension<?> extension, Object point, int sequence) {
        Object source = point == null ? extension : point;
        Map<String, Object> row = identifiableRow(identifiable,
                "elementType", identifiable.getType().name(),
                "extensionType", extension.getClass().getSimpleName(),
                "canonicalId", canonicalId(identifiable.getId()),
                "sequenceNumber", sequence,
                "latitude", firstAvailable(source, "getLatitude", "getLat", "getY", "getYPosition"),
                "longitude", firstAvailable(source, "getLongitude", "getLon", "getLng", "getX", "getXPosition"),
                "xPosition", firstAvailable(source, "getXPosition", "getX"),
                "yPosition", firstAvailable(source, "getYPosition", "getY"),
                "zPosition", firstAvailable(source, "getZPosition", "getZ"));
        row.put("rowId", identifiable.getId() + ":" + extension.getClass().getSimpleName() + ":" + sequence);
        return row;
    }

    private String canonicalId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        int hash = normalized.lastIndexOf('#');
        int slash = normalized.lastIndexOf('/');
        int index = Math.max(hash, slash);
        if (index >= 0 && index < normalized.length() - 1) {
            normalized = normalized.substring(index + 1);
        }
        while (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private Object firstAvailable(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invoke(target, methodName);
            if (value != null) {
                return displayValue(value);
            }
        }
        return "";
    }

    private Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Map<String, Object> identifiableRow(Identifiable<?> identifiable, Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", identifiable.getId());
        row.put("name", identifiable.getOptionalName().orElse(""));
        row.put("type", identifiable.getType().name());
        for (int index = 0; index < values.length; index += 2) {
            row.put(String.valueOf(values[index]), displayValue(values[index + 1]));
        }
        return row;
    }

    private String voltageLevelId(Terminal terminal) {
        return terminal == null || terminal.getVoltageLevel() == null ? "" : terminal.getVoltageLevel().getId();
    }

    private Object displayValue(Object value) {
        if (value instanceof Double number && !Double.isFinite(number)) {
            return "";
        }
        if (value instanceof Float number && !Float.isFinite(number)) {
            return "";
        }
        return value;
    }

    private <T> List<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).toList();
    }
}
