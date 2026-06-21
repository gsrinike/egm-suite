package eu.egm.com.data.cgm;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Projects a PowSyBl IIDM network into the suite's stable IIDM DTOs.
 */
public class PowsyblIidmEquipmentReader {

    public List<IidmEquipment> read(Network network) {
        List<IidmEquipment> equipment = new ArrayList<>();
        addAll(equipment, network.getSubstations(), substation -> equipment(substation, IidmEquipmentType.SUBSTATION, null, 0));
        addAll(equipment, network.getVoltageLevels(), voltageLevel -> equipment(voltageLevel, IidmEquipmentType.VOLTAGE_LEVEL, containerId(voltageLevel), voltageLevel.getNominalV()));
        addAll(equipment, network.getBusbarSections(), busbarSection -> equipment(busbarSection, IidmEquipmentType.BUS, containerId(busbarSection), nominalVoltage(busbarSection)));
        addAll(equipment, network.getLines(), line -> equipment(line, IidmEquipmentType.LINE, branchContainerId(line), branchNominalVoltage(line)));
        addAll(equipment, network.getTwoWindingsTransformers(), transformer -> equipment(transformer, IidmEquipmentType.TWO_WINDINGS_TRANSFORMER, containerId(transformer), branchNominalVoltage(transformer)));
        addAll(equipment, network.getThreeWindingsTransformers(), transformer -> equipment(transformer, IidmEquipmentType.TWO_WINDINGS_TRANSFORMER, containerId(transformer), nominalVoltage(transformer)));
        addAll(equipment, network.getGenerators(), generator -> equipment(generator, IidmEquipmentType.GENERATOR, containerId(generator), nominalVoltage(generator)));
        addAll(equipment, network.getLoads(), load -> equipment(load, IidmEquipmentType.LOAD, containerId(load), nominalVoltage(load)));
        addAll(equipment, network.getShuntCompensators(), shunt -> equipment(shunt, IidmEquipmentType.SHUNT_COMPENSATOR, containerId(shunt), nominalVoltage(shunt)));
        addAll(equipment, network.getSwitches(), networkSwitch -> equipment(networkSwitch, IidmEquipmentType.SWITCH, containerId(networkSwitch), nominalVoltage(networkSwitch)));
        return equipment;
    }

    private <T> void addAll(List<IidmEquipment> equipment, Iterable<T> source, Function<T, IidmEquipment> projector) {
        StreamSupport.stream(source.spliterator(), false)
                .map(projector)
                .forEach(equipment::add);
    }

    private IidmEquipment equipment(Identifiable<?> identifiable, IidmEquipmentType type, String containerId, double nominalVoltage) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("source", "powsybl-cgmes-import");
        attributes.put("iidmType", identifiable.getType().name());
        return new IidmEquipment(identifiable.getId(), identifiable.getNameOrId(), type, containerId, nominalVoltage, attributes);
    }

    private String containerId(VoltageLevel voltageLevel) {
        return voltageLevel.getSubstation().map(Substation::getId).orElse(null);
    }

    private String containerId(TwoWindingsTransformer transformer) {
        return transformer.getSubstation().map(Substation::getId).orElseGet(() -> containerId(transformer.getTerminal1()));
    }

    private String containerId(ThreeWindingsTransformer transformer) {
        return transformer.getSubstation().map(Substation::getId).orElseGet(() -> containerId(transformer.getLeg1().getTerminal()));
    }

    private String branchContainerId(Branch<?> branch) {
        return containerId(branch.getTerminal1());
    }

    private String containerId(Injection<?> injection) {
        return containerId(injection.getTerminal());
    }

    private String containerId(Connectable<?> connectable) {
        return connectable.getTerminals().stream()
                .findFirst()
                .map(this::containerId)
                .orElse(null);
    }

    private String containerId(Switch networkSwitch) {
        return networkSwitch.getVoltageLevel().getId();
    }

    private String containerId(Terminal terminal) {
        return terminal.getVoltageLevel().getId();
    }

    private double branchNominalVoltage(Branch<?> branch) {
        return nominalVoltage(branch.getTerminal1());
    }

    private double nominalVoltage(Injection<?> injection) {
        return nominalVoltage(injection.getTerminal());
    }

    private double nominalVoltage(Connectable<?> connectable) {
        return connectable.getTerminals().stream()
                .findFirst()
                .map(this::nominalVoltage)
                .orElse(0D);
    }

    private double nominalVoltage(Switch networkSwitch) {
        return networkSwitch.getVoltageLevel().getNominalV();
    }

    private double nominalVoltage(Terminal terminal) {
        return terminal.getVoltageLevel().getNominalV();
    }
}
