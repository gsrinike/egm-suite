package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.snapshot.CgmNetworkSnapshot;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RdfStreamingSnapshotAssemblyTest {
    @Test
    void streamsRdfIntoProfileFragmentAndAssemblesSnapshot() {
        RdfMetadata metadata = new RdfMetadataExtractor().extract(
                rdf("Equipment"),
                ProfileProcessingContext.forFile(
                        "import-1",
                        "file-1",
                        "object-1",
                        "TSO-XYZ",
                        "2024-12-03",
                        "00:30",
                        "1D",
                        ProfileFamily.CGMES,
                        "EQ"));

        assertThat(metadata.fragment().facts()).isNotEmpty();
        assertThat(metadata.fragment().facts())
                .extracting(fact -> fact.mRID())
                .contains("_substation_1", "_vl_1");

        CgmNetworkSnapshot snapshot = new CgmSnapshotAssembler().assemble(CnmServiceType.CGM, List.of(metadata.fragment()));

        assertThat(snapshot.snapshotId()).isEqualTo("import-1:TSO-XYZ:2024-12-03:00:30:1D");
        assertThat(snapshot.staticTopology().objects()).isNotEmpty();
        assertThat(snapshot.sourceFileIds()).containsExactly("file-1");
    }

    @Test
    void acceptsDigitStartingRdfIdsAndAddsCanonicalIds() {
        RdfMetadata metadata = new RdfMetadataExtractor().extract(
                glRdfWithDigitStartingIds(),
                ProfileProcessingContext.forFile(
                        "import-1",
                        "file-gl",
                        "object-gl",
                        "TSO-XYZ",
                        "2024-12-03",
                        "00:30",
                        "1D",
                        ProfileFamily.CGMES,
                        "GL"));

        assertThat(metadata.fragment().facts())
                .filteredOn(fact -> "PositionPoint".equals(fact.cimType()))
                .singleElement()
                .satisfies(fact -> assertThat(fact.attributes())
                        .containsEntry("mRID", "93bfbd5b-b901-47d0-9eda-6a194b76cea5")
                        .containsEntry("canonicalMRID", "93bfbd5b-b901-47d0-9eda-6a194b76cea5"));
        assertThat(metadata.fragment().facts())
                .filteredOn(fact -> "Location".equals(fact.cimType()))
                .singleElement()
                .satisfies(fact -> assertThat(fact.references())
                        .containsEntry("PowerSystemResources", "c31891c5-540d-4a68-8116-8b5ff781c63a")
                        .containsEntry("canonicalPowerSystemResources", "c31891c5-540d-4a68-8116-8b5ff781c63a"));
    }

    private byte[] rdf(String profileName) {
        return ("""
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:md="http://iec.ch/TC57/61970-552/ModelDescription/1#"
                         xmlns:cim="http://iec.ch/TC57/CIM100#"
                         xmlns:dcterms="http://purl.org/dc/terms/">
                  <md:FullModel rdf:about="_model">
                    <dcterms:conformsTo rdf:resource="http://entsoe.eu/CIM/CGMES/3.0/%s/1"/>
                  </md:FullModel>
                  <cim:Substation rdf:ID="_substation_1">
                    <cim:IdentifiedObject.mRID>_substation_1</cim:IdentifiedObject.mRID>
                    <cim:IdentifiedObject.name>Substation 1</cim:IdentifiedObject.name>
                  </cim:Substation>
                  <cim:VoltageLevel rdf:ID="_vl_1">
                    <cim:IdentifiedObject.mRID>_vl_1</cim:IdentifiedObject.mRID>
                    <cim:IdentifiedObject.name>Voltage Level 1</cim:IdentifiedObject.name>
                    <cim:VoltageLevel.Substation rdf:resource="#_substation_1"/>
                  </cim:VoltageLevel>
                </rdf:RDF>
                """.formatted(profileName)).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] glRdfWithDigitStartingIds() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:md="http://iec.ch/TC57/61970-552/ModelDescription/1#"
                         xmlns:cim="http://iec.ch/TC57/CIM100#"
                         xmlns:nc="http://entsoe.eu/ns/nc#"
                         xmlns:dcterms="http://purl.org/dc/terms/">
                  <md:FullModel rdf:about="_model">
                    <dcterms:conformsTo rdf:resource="http://entsoe.eu/CIM/CGMES/3.0/GeographicalLocation/1"/>
                  </md:FullModel>
                  <cim:CoordinateSystem rdf:ID="9789cd49-4957-4192-957f-45f1de168be5">
                    <cim:IdentifiedObject.mRID>9789cd49-4957-4192-957f-45f1de168be5</cim:IdentifiedObject.mRID>
                  </cim:CoordinateSystem>
                  <cim:PositionPoint rdf:ID="93bfbd5b-b901-47d0-9eda-6a194b76cea5">
                    <cim:IdentifiedObject.mRID>93bfbd5b-b901-47d0-9eda-6a194b76cea5</cim:IdentifiedObject.mRID>
                    <nc:PositionPoint.xPosition>18.508337</nc:PositionPoint.xPosition>
                    <nc:PositionPoint.yPosition>49.784776</nc:PositionPoint.yPosition>
                    <nc:PositionPoint.Location rdf:resource="#_location-1"/>
                  </cim:PositionPoint>
                  <cim:Location rdf:ID="_location-1">
                    <cim:IdentifiedObject.mRID>_location-1</cim:IdentifiedObject.mRID>
                    <nc:Location.PowerSystemResources rdf:resource="#c31891c5-540d-4a68-8116-8b5ff781c63a"/>
                    <nc:Location.CoordinateSystem rdf:resource="#9789cd49-4957-4192-957f-45f1de168be5"/>
                  </cim:Location>
                </rdf:RDF>
                """.getBytes(StandardCharsets.UTF_8);
    }
}
