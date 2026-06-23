package eu.egm.srv.cgm.importer;

import com.infra.InfrastructureUtils;
import com.infra.event.EventPublisherService;
import eu.egm.data.cgm.dto.cgmes.CgmesConstants;
import eu.egm.data.cgm.dto.cgmes.CgmesProcess;
import eu.egm.data.cgm.dto.cgmes.CgmesRegion;
import eu.egm.data.cgm.dto.cgmes.ImportStatus;
import eu.egm.srv.cgm.importer.domain.ImportStatusDocument;
import eu.egm.srv.cgm.importer.repository.EquipmentSearchRepository;
import eu.egm.srv.cgm.importer.repository.ImportStatusRepository;
import eu.egm.srv.cgm.importer.service.CgmImportService;
import eu.egm.srv.cgm.importer.service.CgmImportStoredObjectsEvent;
import eu.egm.srv.cgm.importer.service.CgmesNetworkReader;
import eu.egm.srv.cgm.importer.service.RawCgmesStorage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CgmImportServiceTest {

    @Test
    void returnsInitAfterParallelObjectStorage() {
        CgmesNetworkReader networkReader = mock(CgmesNetworkReader.class);
        EquipmentSearchRepository equipmentRepository = mock(EquipmentSearchRepository.class);
        ImportStatusRepository statusRepository = mock(ImportStatusRepository.class);
        RawCgmesStorage rawStorage = mock(RawCgmesStorage.class);
        EventPublisherService eventPublisher = mock(EventPublisherService.class);
        InfrastructureUtils infrastructureUtils = mock(InfrastructureUtils.class);
        CgmImportService service = new CgmImportService(networkReader, equipmentRepository, statusRepository, rawStorage, eventPublisher, infrastructureUtils);

        ImportStatus status = service.importCgmes(List.of(
                file("20231016T0030Z_1D_TSCNET-EU-MAVIR_EQ_001.xml"),
                file("20231016T0030Z_1D_TSCNET-EU-MAVIR_TP_001.xml")
        ), CgmesRegion.CORE, CgmesProcess.CGM);

        assertThat(status.state()).isEqualTo("Init");
        assertThat(status.indexedEquipmentCount()).isZero();
        assertThat(status.fileName()).contains("EQ_001.xml", "TP_001.xml");
        assertThat(status.files()).hasSize(2);
        assertThat(status.files()).extracting("status").containsOnly("Init");
        verify(rawStorage).store(eq(status.networkId() + "/1-20231016T0030Z_1D_TSCNET-EU-MAVIR_EQ_001.xml"), any(), eq("application/xml"));
        verify(rawStorage).store(eq(status.networkId() + "/2-20231016T0030Z_1D_TSCNET-EU-MAVIR_TP_001.xml"), any(), eq("application/xml"));
        ArgumentCaptor<CgmImportStoredObjectsEvent> storedEvent = ArgumentCaptor.forClass(CgmImportStoredObjectsEvent.class);
        verify(eventPublisher).publish(eq(CgmesConstants.IMPORT_EXCHANGE), eq(CgmesConstants.IMPORT_STORED_ROUTING_KEY),
                storedEvent.capture());
        assertThat(storedEvent.getValue().networkId()).isEqualTo(status.networkId());
        assertThat(storedEvent.getValue().objectIds()).containsExactly(
                status.networkId() + "/1-20231016T0030Z_1D_TSCNET-EU-MAVIR_EQ_001.xml",
                status.networkId() + "/2-20231016T0030Z_1D_TSCNET-EU-MAVIR_TP_001.xml");
        verify(equipmentRepository, never()).saveAll(any());
        verify(statusRepository).save(any(ImportStatusDocument.class));
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "application/xml", "<rdf:RDF/>".getBytes());
    }
}
