package eu.egm.com.data.cgmes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentClassifierTest {
    @Test
    void classifiesCommonCgmesClasses() {
        assertThat(EquipmentClassifier.fromProfileClass("cim:ACLineSegment")).isEqualTo(EquipmentType.LINE);
        assertThat(EquipmentClassifier.fromProfileClass("cim:SynchronousMachine")).isEqualTo(EquipmentType.GENERATOR);
        assertThat(EquipmentClassifier.fromProfileClass("cim:Breaker")).isEqualTo(EquipmentType.SWITCH);
        assertThat(EquipmentClassifier.fromProfileClass("custom:Thing")).isEqualTo(EquipmentType.UNKNOWN);
    }

    @Test
    void parsesCgmesFileNamingConvention() {
        ImportMetadata metadata = CgmesFileNameParser.parse("20231016T0030Z_1D_TSCNET-EU-MAVIR_SSH_000.zip", CgmesRegion.CORE, CgmesProcess.CGM);

        assertThat(metadata.businessDay().toString()).isEqualTo("2023-10-16");
        assertThat(metadata.timestamp().toString()).isEqualTo("00:30");
        assertThat(metadata.timeFrame()).isEqualTo("1D");
        assertThat(metadata.tsoName()).isEqualTo("TSCNET-EU-MAVIR");
        assertThat(metadata.cgmesProfileType()).isEqualTo("SSH");
        assertThat(metadata.powsyblProfileType()).isEqualTo(CgmesProfileType.SSH);
        assertThat(metadata.powsyblProfileType().powsyblSubset().getIdentifier()).isNotBlank();
        assertThat(metadata.versionNumber()).isEqualTo("000");
        assertThat(metadata.extension()).isEqualTo("zip");
    }

    @Test
    void exposesPowsyblCgmesCatalog() {
        assertThat(PowsyblCgmesModelDefinition.SUPPORTED_SUBSETS)
                .contains(CgmesProfileType.EQ.powsyblSubset(), CgmesProfileType.SSH.powsyblSubset());
        assertThat(PowsyblCgmesModelDefinition.SEARCHABLE_CIM_TYPES).isNotEmpty();
    }
}
