package eu.egm.srv.cgm.importer;

import eu.egm.data.cgm.dto.cgmes.EquipmentType;
import eu.egm.data.cgm.dto.cgmes.CgmesProcess;
import eu.egm.data.cgm.dto.cgmes.CgmesRegion;
import eu.egm.data.cgm.dto.cgmes.ImportMetadata;
import eu.egm.srv.cgm.importer.service.PowsyblCgmesNetworkReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PowsyblCgmesNetworkReaderTest {
    private static final ImportMetadata METADATA = ImportMetadata.of(java.time.LocalDate.parse("2023-10-16"), "00:30", CgmesRegion.CORE, CgmesProcess.CGM);

    @Test
    void readsCgmesEquipmentFromRdfXml() {
        String xml = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:cim="http://iec.ch/TC57/2013/CIM-schema-cim16#">
                  <cim:Substation rdf:ID="SUB1">
                    <cim:IdentifiedObject.name>Main substation</cim:IdentifiedObject.name>
                  </cim:Substation>
                  <cim:ACLineSegment rdf:ID="LINE1">
                    <cim:IdentifiedObject.name>Line 1</cim:IdentifiedObject.name>
                    <cim:Equipment.EquipmentContainer rdf:resource="#SUB1"/>
                  </cim:ACLineSegment>
                </rdf:RDF>
                """;

        var equipment = new PowsyblCgmesNetworkReader()
                .read("network-a", METADATA, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertThat(equipment).hasSize(2);
        assertThat(equipment).anySatisfy(view -> {
            assertThat(view.id()).isEqualTo("LINE1");
            assertThat(view.type()).isEqualTo(EquipmentType.LINE);
            assertThat(view.containerId()).isEqualTo("SUB1");
        });
    }

    @Test
    void readsCgmesXmlFromZipPayload() throws Exception {
        String xml = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:cim="http://iec.ch/TC57/2013/CIM-schema-cim16#">
                  <cim:SvVoltage rdf:ID="_sv1">
                    <cim:SvVoltage.v>230</cim:SvVoltage.v>
                    <cim:SvVoltage.TopologicalNode rdf:resource="#_node1"/>
                  </cim:SvVoltage>
                </rdf:RDF>
                """;

        var equipment = new PowsyblCgmesNetworkReader()
                .read("network-a", METADATA, new ByteArrayInputStream(zip("sv.xml", xml)));

        assertThat(equipment).hasSize(1);
        assertThat(equipment.getFirst().id()).isEqualTo("_sv1");
        assertThat(equipment.getFirst().type()).isEqualTo(EquipmentType.STATE_VARIABLE);
        assertThat(equipment.getFirst().metadata()).isEqualTo(METADATA);
    }

    private byte[] zip(String name, String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(bytes)) {
            zipOutputStream.putNextEntry(new ZipEntry(name));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return bytes.toByteArray();
    }
}
