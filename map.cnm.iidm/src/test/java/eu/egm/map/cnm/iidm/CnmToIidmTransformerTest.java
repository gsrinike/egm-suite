package eu.egm.map.cnm.iidm;

import eu.egm.data.cnm.common.GridTopologyObject;
import eu.egm.data.cnm.common.GridTopologyRelation;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.iidm.network.IidmNetworkXiidm;
import eu.egm.mapping.ReflectionMappingService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CnmToIidmTransformerTest {
    @Test
    void mapsCommonTopologyAndStateVariablesIntoIidmProjection() {
        CnmToIidmTransformer transformer =
                new CnmToIidmTransformer(new ReflectionMappingService(), new CnmToIidmMappingConfiguration());
        ProfilePayload<Object> payload = new ProfilePayload<>(
                ProfileFamily.CGMES,
                "SV",
                "file-1",
                "object-1",
                List.of(
                        new GridTopologyObject("S1", "Substation", "Substation", "EQ", Map.of()),
                        new GridTopologyObject("VL1", "VL", "VoltageLevel", "EQ", Map.of("nominalVoltage", "400")),
                        new GridTopologyObject("TN1", "Bus 1", "TopologicalNode", "TP", Map.of()),
                        new GridTopologyObject("TN2", "Bus 2", "TopologicalNode", "TP", Map.of()),
                        new GridTopologyObject("L1", "Line", "ACLineSegment", "EQ", Map.of()),
                        new GridTopologyObject("T1", "Terminal", "Terminal", "EQ", Map.of()),
                        new GridTopologyObject("T2", "Terminal", "Terminal", "EQ", Map.of()),
                        new GridTopologyObject("SV1", "", "SvVoltage", "SV", Map.of("v", "410.2", "angle", "-1.1"))),
                List.of(
                        new GridTopologyRelation("r1", "VL1", "S1", "VoltageLevel.Substation", Map.of()),
                        new GridTopologyRelation("r2", "TN1", "VL1", "TopologicalNode.VoltageLevel", Map.of()),
                        new GridTopologyRelation("r3", "TN2", "VL1", "TopologicalNode.VoltageLevel", Map.of()),
                        new GridTopologyRelation("r3", "SV1", "TN1", "SvVoltage.TopologicalNode", Map.of()),
                        new GridTopologyRelation("r4", "T1", "L1", "Terminal.ConductingEquipment", Map.of()),
                        new GridTopologyRelation("r5", "T1", "TN1", "Terminal.TopologicalNode", Map.of()),
                        new GridTopologyRelation("r6", "T2", "L1", "Terminal.ConductingEquipment", Map.of()),
                        new GridTopologyRelation("r7", "T2", "TN2", "Terminal.TopologicalNode", Map.of())),
                List.of(),
                Map.of());

        var network = transformer.transform(payload, "import-1", "2024-12-02", "23:30", "1D", "TSO-XYZ");

        assertThat(network.network().getSubstation("S1")).isNotNull();
        assertThat(network.network().getVoltageLevel("VL1").getNominalV()).isEqualTo(400.0);
        assertThat(network.network().getBusBreakerView().getBus("TN1").getV()).isEqualTo(410.2);
        assertThat(network.network().getLine("L1")).isNotNull();
        assertThat(network.summary().lineCount()).isEqualTo(1);

        var roundTrip = IidmNetworkXiidm.read(IidmNetworkXiidm.write(network.network()));
        assertThat(roundTrip.getLine("L1")).isNotNull();
    }
}
