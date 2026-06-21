package eu.egm.srv.cgm.importer;

import eu.egm.com.data.cgm.CgmesProcess;
import eu.egm.com.data.cgm.CgmesRegion;
import eu.egm.com.data.cgm.EquipmentType;
import eu.egm.com.data.cgm.EquipmentView;
import eu.egm.com.data.cgm.ImportMetadata;
import eu.egm.com.data.cgm.IidmEquipmentType;
import eu.egm.srv.cgm.importer.domain.EquipmentDocument;
import eu.egm.srv.cgm.importer.repository.EquipmentSearchRepository;
import eu.egm.srv.cgm.importer.service.IidmConversionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IidmConversionServiceTest {
    @Test
    void convertsPersistedCgmesProjectionToIidmNetwork() {
        EquipmentSearchRepository repository = mock(EquipmentSearchRepository.class);
        ImportMetadata metadata = ImportMetadata.of(LocalDate.parse("2023-10-16"), "00:30", CgmesRegion.CORE, CgmesProcess.CGM);
        when(repository.findByNetworkId("network-a")).thenReturn(List.of(
                EquipmentDocument.fromView(new EquipmentView("tr-1", "network-a", metadata, "Transformer", EquipmentType.TRANSFORMER, "vl-1", 400.0, Map.of()))
        ));

        var network = new IidmConversionService(repository).convert("network-a");

        assertThat(network.id()).isEqualTo("network-a");
        assertThat(network.equipment()).singleElement()
                .extracting(equipment -> equipment.type())
                .isEqualTo(IidmEquipmentType.TWO_WINDINGS_TRANSFORMER);
    }
}
