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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
