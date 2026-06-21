package eu.egm.data.cgm.dto.iidm;

import com.powsybl.iidm.network.extensions.OperatingStatus;
import eu.egm.data.cgm.mapping.PowsyblIidmExtensionDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IidmExtensionTypeTest {
    @Test
    void alignsExtensionNamesWithPowsybl() {
        assertThat(IidmExtensionType.OPERATING_STATUS.powsyblName()).isEqualTo(OperatingStatus.NAME);
        assertThat(IidmExtensionType.OPERATING_STATUS.powsyblExtensionType()).isEqualTo(OperatingStatus.class);
        assertThat(PowsyblIidmExtensionDefinition.SUPPORTED_EXTENSIONS).contains(IidmExtensionType.MEASUREMENTS);
    }

    @Test
    void carriesExtensionValuesOnEquipment() {
        IidmExtensionValue extension = new IidmExtensionValue(IidmExtensionType.OPERATING_STATUS, null, Map.of("status", "IN_OPERATION"));
        IidmEquipment equipment = new IidmEquipment("id", "name", IidmEquipmentType.LINE, "container", 400.0, java.util.List.of(extension), Map.of());

        assertThat(equipment.extensions()).singleElement()
                .extracting(IidmExtensionValue::powsyblName)
                .isEqualTo(OperatingStatus.NAME);
    }
}
