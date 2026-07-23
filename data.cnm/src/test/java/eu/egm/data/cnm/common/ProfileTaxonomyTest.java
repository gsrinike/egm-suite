package eu.egm.data.cnm.common;

import eu.egm.data.cnm.cgmes.CgmesProfileKind;
import eu.egm.data.cnm.nc.NCProfileKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileTaxonomyTest {
    @Test
    void profileFamilyContainsOnlyTopLevelFamilies() {
        assertThat(ProfileFamily.values())
                .containsExactly(ProfileFamily.NCP, ProfileFamily.CGMES, ProfileFamily.Unknown);
    }

    @Test
    void cgmesKindsRecognizeCoreAndLayoutProfiles() {
        assertThat(CgmesProfileKind.fromCode("EQ")).isEqualTo(CgmesProfileKind.EQUIPMENT);
        assertThat(CgmesProfileKind.fromCode("SSH")).isEqualTo(CgmesProfileKind.STEADY_STATE_HYPOTHESIS);
        assertThat(CgmesProfileKind.fromCode("SV")).isEqualTo(CgmesProfileKind.STATE_VARIABLES);
        assertThat(CgmesProfileKind.fromCode("TP")).isEqualTo(CgmesProfileKind.TOPOLOGY);
        assertThat(CgmesProfileKind.fromCode("DL")).isEqualTo(CgmesProfileKind.DIAGRAM_LAYOUT);
        assertThat(CgmesProfileKind.fromCode("GL")).isEqualTo(CgmesProfileKind.GEOGRAPHICAL_LOCATION);
    }

    @Test
    void ncKindsRecognizeRegionalCoordinationProfiles() {
        assertThat(NCProfileKind.fromCode("AEAS")).isEqualTo(NCProfileKind.ASSESSED_ELEMENT_AVAILABILITY_SCHEDULE);
        assertThat(NCProfileKind.fromCode("ER")).isEqualTo(NCProfileKind.EQUIPMENT_RELIABILITY);
        assertThat(NCProfileKind.fromCode("SAR")).isEqualTo(NCProfileKind.SECURITY_ANALYSIS_RESULTS);
        assertThat(NCProfileKind.fromCode("SHS")).isEqualTo(NCProfileKind.STEADY_STATE_HYPOTHESIS_SCHEDULE);
        assertThat(NCProfileKind.fromCode("SSI")).isEqualTo(NCProfileKind.STEADY_STATE_INSTRUCTION);
    }
}
