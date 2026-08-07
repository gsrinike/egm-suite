package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.TimeFrame;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CnmModelFileNameParserTest {
    @Test
    void parsesCanonicalOperationalProfileName() {
        CnmModelFileNameParser.CnmModelFileName metadata = CnmModelFileNameParser.parse(
                "20241203T0430Z_1D_TTN_EQ_000.zip",
                TimeFrame.DAY_AHEAD);

        assertThat(metadata.businessDay()).isEqualTo("2024-12-03");
        assertThat(metadata.businessTime()).isEqualTo("04:30");
        assertThat(metadata.timeFrame()).isEqualTo("1D");
        assertThat(metadata.tsoName()).isEqualTo("TTN");
        assertThat(metadata.profileType()).isEqualTo("EQ");
        assertThat(metadata.version()).isEqualTo("000");
        assertThat(metadata.profileFamily()).isEqualTo(ProfileFamily.CGMES);
    }

    @Test
    void parsesBoundaryProfileWithoutFilenameTimeframe() {
        CnmModelFileNameParser.CnmModelFileName metadata = CnmModelFileNameParser.parse(
                "20241203T0430Z__ENTSOE_EQBD_031.zip",
                TimeFrame.DAY_AHEAD);

        assertThat(metadata.businessDay()).isEqualTo("2024-12-03");
        assertThat(metadata.businessTime()).isEqualTo("04:30");
        assertThat(metadata.timeFrame()).isEqualTo("1D");
        assertThat(metadata.tsoName()).isEqualTo("ENTSOE");
        assertThat(metadata.profileType()).isEqualTo("EQ_BD");
        assertThat(metadata.version()).isEqualTo("031");
        assertThat(metadata.profileFamily()).isEqualTo(ProfileFamily.CGMES);
    }

    @Test
    void parsesBoundaryTopologyProfileWithoutFilenameTimeframe() {
        CnmModelFileNameParser.CnmModelFileName metadata = CnmModelFileNameParser.parse(
                "20241203T0430Z__ENTSOE_TPBD_031.zip",
                TimeFrame.DAY_AHEAD);

        assertThat(metadata.profileType()).isEqualTo("TP_BD");
        assertThat(metadata.timeFrame()).isEqualTo("1D");
        assertThat(metadata.tsoName()).isEqualTo("ENTSOE");
    }

    @Test
    void parsesGeographicalLocationProfileWithChainedXmlZipExtension() {
        CnmModelFileNameParser.CnmModelFileName metadata = CnmModelFileNameParser.parse(
                "20241203T0030Z__CEPS_GL_001.xml.zip",
                TimeFrame.DAY_AHEAD);

        assertThat(metadata.businessDay()).isEqualTo("2024-12-03");
        assertThat(metadata.businessTime()).isEqualTo("00:30");
        assertThat(metadata.timeFrame()).isEqualTo("1D");
        assertThat(metadata.tsoName()).isEqualTo("CEPS");
        assertThat(metadata.profileType()).isEqualTo("GL");
        assertThat(metadata.version()).isEqualTo("001");
        assertThat(metadata.profileFamily()).isEqualTo(ProfileFamily.CGMES);
    }

    @Test
    void parsesGeographicalLocationProfileForNumericTsoAliases() {
        CnmModelFileNameParser.CnmModelFileName metadata = CnmModelFileNameParser.parse(
                "/models/20241001T0030Z__D4_GL_002.xml.zip",
                TimeFrame.DAY_AHEAD);

        assertThat(metadata.businessDay()).isEqualTo("2024-10-01");
        assertThat(metadata.businessTime()).isEqualTo("00:30");
        assertThat(metadata.tsoName()).isEqualTo("D4");
        assertThat(metadata.profileType()).isEqualTo("GL");
        assertThat(metadata.version()).isEqualTo("002");
    }
}
