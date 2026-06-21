package eu.egm.srv.cgm.importer;

import com.infra.document.DocumentPage;
import eu.egm.com.data.cgm.CgmesProcess;
import eu.egm.com.data.cgm.CgmesRegion;
import eu.egm.com.data.cgm.EquipmentType;
import eu.egm.com.data.cgm.EquipmentView;
import eu.egm.com.data.cgm.ImportMetadata;
import eu.egm.com.data.cgm.SearchRequest;
import eu.egm.srv.cgm.importer.domain.EquipmentDocument;
import eu.egm.srv.cgm.importer.repository.EquipmentSearchRepository;
import eu.egm.srv.cgm.importer.service.EquipmentQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EquipmentQueryServiceTest {
    private static final ImportMetadata METADATA = ImportMetadata.of(java.time.LocalDate.parse("2023-10-16"), "00:30", CgmesRegion.CORE, CgmesProcess.CGM);
    private static final ImportMetadata OTHER_METADATA = ImportMetadata.of(java.time.LocalDate.parse("2023-10-17"), "00:45", CgmesRegion.HANSA, CgmesProcess.CSA);

    @Test
    void filtersByTypeAndQuery() {
        EquipmentSearchRepository repository = mock(EquipmentSearchRepository.class);
        SearchRequest request = new SearchRequest("north", EquipmentType.LINE, null, CgmesRegion.CORE, CgmesProcess.CGM, "2023-10-16", "00:30", null, null, null, null, null, 0, 20);
        when(repository.search("n1", request)).thenReturn(new DocumentPage<>(List.of(
                EquipmentDocument.fromView(new EquipmentView("L1", "n1", METADATA, "North line", EquipmentType.LINE, "S1", 225, Map.of()))
        ), 1, 0, 20));

        var response = new EquipmentQueryService(repository)
                .search("n1", request);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.content().getFirst().id()).isEqualTo("L1");
    }

    @Test
    void comparesNetworkStates() {
        EquipmentSearchRepository repository = mock(EquipmentSearchRepository.class);
        when(repository.findByNetworkId("base")).thenReturn(List.of(
                EquipmentDocument.fromView(new EquipmentView("L1", "base", METADATA, "Line", EquipmentType.LINE, "S1", 225, Map.of("r", "1")))
        ));
        when(repository.findByNetworkId("study")).thenReturn(List.of(
                EquipmentDocument.fromView(new EquipmentView("L1", "study", METADATA, "Line", EquipmentType.LINE, "S1", 400, Map.of("r", "1"))),
                EquipmentDocument.fromView(new EquipmentView("G1", "study", METADATA, "Generator", EquipmentType.GENERATOR, "S1", 20, Map.of()))
        ));

        var diff = new EquipmentQueryService(repository).compare("base", "study");

        assertThat(diff.added()).extracting(EquipmentView::id).containsExactly("G1");
        assertThat(diff.changed()).hasSize(1);
        assertThat(diff.changed().getFirst().changedFields()).contains("nominalVoltage");
    }
}
