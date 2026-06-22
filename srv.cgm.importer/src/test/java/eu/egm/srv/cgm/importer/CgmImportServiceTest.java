package eu.egm.srv.cgm.importer;

import com.infra.event.EventPublisherService;
import eu.egm.data.cgm.dto.cgmes.CgmesConstants;
import eu.egm.data.cgm.dto.cgmes.CgmesProcess;
import eu.egm.data.cgm.dto.cgmes.CgmesRegion;
import eu.egm.data.cgm.dto.cgmes.EquipmentType;
import eu.egm.data.cgm.dto.cgmes.EquipmentView;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CgmImportServiceTest {

    @Test
    void returnsInProgressAfterStorageAndCompletesBackgroundProcessing() throws Exception {
        CgmesNetworkReader networkReader = mock(CgmesNetworkReader.class);
        EquipmentSearchRepository equipmentRepository = mock(EquipmentSearchRepository.class);
        ImportStatusRepository statusRepository = mock(ImportStatusRepository.class);
        RawCgmesStorage rawStorage = mock(RawCgmesStorage.class);
        EventPublisherService eventPublisher = mock(EventPublisherService.class);
        CgmImportService service = new CgmImportService(networkReader, equipmentRepository, statusRepository, rawStorage, eventPublisher);
        CountDownLatch readerStarted = new CountDownLatch(2);
        CountDownLatch releaseReader = new CountDownLatch(1);

        when(networkReader.read(anyString(), any(), any())).thenAnswer(invocation -> {
            String networkId = invocation.getArgument(0);
            readerStarted.countDown();
            assertThat(releaseReader.await(2, TimeUnit.SECONDS)).isTrue();
            return List.of(new EquipmentView("eq-" + readerStarted.getCount(), networkId, invocation.getArgument(1),
                    "Equipment", EquipmentType.LINE, "container", 400.0, Map.of()));
        });

        ImportStatus status = service.importCgmes(List.of(
                file("20231016T0030Z_1D_TSCNET-EU-MAVIR_EQ_001.xml"),
                file("20231016T0030Z_1D_TSCNET-EU-MAVIR_TP_001.xml")
        ), CgmesRegion.CORE, CgmesProcess.CGM);

        assertThat(status.state()).isEqualTo("In Progress");
        assertThat(status.indexedEquipmentCount()).isZero();
        assertThat(status.fileName()).contains("EQ_001.xml", "TP_001.xml");
        verify(rawStorage).store(eq(status.networkId() + "/1-20231016T0030Z_1D_TSCNET-EU-MAVIR_EQ_001.xml"), any(), eq("application/xml"));
        verify(rawStorage).store(eq(status.networkId() + "/2-20231016T0030Z_1D_TSCNET-EU-MAVIR_TP_001.xml"), any(), eq("application/xml"));
        ArgumentCaptor<CgmImportStoredObjectsEvent> storedEvent = ArgumentCaptor.forClass(CgmImportStoredObjectsEvent.class);
        verify(eventPublisher).publish(eq(CgmesConstants.IMPORT_EXCHANGE), eq(CgmesConstants.IMPORT_STORED_ROUTING_KEY),
                storedEvent.capture());
        assertThat(storedEvent.getValue().networkId()).isEqualTo(status.networkId());
        assertThat(storedEvent.getValue().objectIds()).containsExactly(
                status.networkId() + "/1-20231016T0030Z_1D_TSCNET-EU-MAVIR_EQ_001.xml",
                status.networkId() + "/2-20231016T0030Z_1D_TSCNET-EU-MAVIR_TP_001.xml");

        assertThat(readerStarted.await(2, TimeUnit.SECONDS)).isTrue();
        releaseReader.countDown();

        verify(equipmentRepository, timeout(2_000).times(2)).saveAll(any());
        verify(statusRepository, timeout(2_000).atLeast(2)).save(any(ImportStatusDocument.class));
        verify(eventPublisher, timeout(2_000)).publish(eq(CgmesConstants.IMPORT_EXCHANGE), eq(CgmesConstants.IMPORTED_ROUTING_KEY),
                any(ImportStatus.class));
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "application/xml", "<rdf:RDF/>".getBytes());
    }
}
