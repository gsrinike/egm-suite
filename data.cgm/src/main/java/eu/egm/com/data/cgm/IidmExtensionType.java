package eu.egm.com.data.cgm;

import com.powsybl.iidm.network.extensions.ActivePowerControl;
import com.powsybl.iidm.network.extensions.BranchObservability;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.iidm.network.extensions.GeneratorEntsoeCategory;
import com.powsybl.iidm.network.extensions.InjectionObservability;
import com.powsybl.iidm.network.extensions.LinePosition;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.iidm.network.extensions.OperatingStatus;
import com.powsybl.iidm.network.extensions.RemoteReactivePowerControl;
import com.powsybl.iidm.network.extensions.SlackTerminal;
import com.powsybl.iidm.network.extensions.SubstationPosition;

import java.util.Arrays;

/**
 * IIDM extension types aligned with PowSyBl's common extension module.
 */
public enum IidmExtensionType {
    OPERATING_STATUS(OperatingStatus.NAME, OperatingStatus.class),
    MEASUREMENTS(Measurements.NAME, Measurements.class),
    CONNECTABLE_POSITION(ConnectablePosition.NAME, ConnectablePosition.class),
    SUBSTATION_POSITION(SubstationPosition.NAME, SubstationPosition.class),
    LINE_POSITION(LinePosition.NAME, LinePosition.class),
    BRANCH_OBSERVABILITY(BranchObservability.NAME, BranchObservability.class),
    INJECTION_OBSERVABILITY(InjectionObservability.NAME, InjectionObservability.class),
    ACTIVE_POWER_CONTROL(ActivePowerControl.NAME, ActivePowerControl.class),
    REMOTE_REACTIVE_POWER_CONTROL(RemoteReactivePowerControl.NAME, RemoteReactivePowerControl.class),
    GENERATOR_ENTSOE_CATEGORY(GeneratorEntsoeCategory.NAME, GeneratorEntsoeCategory.class),
    LOAD_DETAIL(LoadDetail.NAME, LoadDetail.class),
    SLACK_TERMINAL(SlackTerminal.NAME, SlackTerminal.class);

    private final String powsyblName;
    private final Class<?> powsyblExtensionType;

    IidmExtensionType(String powsyblName, Class<?> powsyblExtensionType) {
        this.powsyblName = powsyblName;
        this.powsyblExtensionType = powsyblExtensionType;
    }

    public String powsyblName() {
        return powsyblName;
    }

    public Class<?> powsyblExtensionType() {
        return powsyblExtensionType;
    }

    public static IidmExtensionType fromPowsyblName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.powsyblName.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
