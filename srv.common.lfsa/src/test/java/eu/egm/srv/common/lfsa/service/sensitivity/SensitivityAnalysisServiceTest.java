package eu.egm.srv.common.lfsa.service.sensitivity;

import com.infra.InfrastructureUtils;
import com.infra.bpm.BusinessProcessService;
import com.infra.event.EventPublisherService;
import com.infra.storage.document.DocumentAdapter;
import com.infra.storage.document.DocumentPage;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSearchRequest;
import com.infra.storage.document.DocumentSort;
import com.infra.storage.object.ObjectStorageService;
import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.IidmTransformationStatus;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.TimeFrame;
import eu.egm.srv.common.lfsa.config.sensitivity.SensitivityDefaultsService;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocument;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocumentAdapter;
import eu.egm.srv.common.lfsa.domain.IidmNetworkReadDocument;
import eu.egm.srv.common.lfsa.domain.IidmNetworkReadDocumentAdapter;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensitivityAnalysisServiceTest {
    @Test
    void completedIidmNetworksRequiresSuccessfulImportWithDoneIidmStatus() {
        CapturingRepository<CnmImportReadDocument> imports = new CapturingRepository<>();
        CapturingRepository<IidmNetworkReadDocument> networks = new CapturingRepository<>();
        imports.save(importDocument("ready-import", ImportState.SUCCESS, IidmTransformationStatus.DONE));
        imports.save(importDocument("not-iidm-ready", ImportState.SUCCESS, IidmTransformationStatus.STARTED));
        networks.save(networkDocument("network-1", "ready-import"));
        SensitivityAnalysisService service = service(imports, networks);

        assertThat(service.completedIidmNetworks("ready-import", 0, 100).items())
                .singleElement()
                .satisfies(network -> {
                    assertThat(network.id()).isEqualTo("network-1");
                    assertThat(network.importId()).isEqualTo("ready-import");
                });
        assertThatThrownBy(() -> service.completedIidmNetworks("not-iidm-ready", 0, 100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ready for sensitivity analysis");
    }

    private static SensitivityAnalysisService service(
            CapturingRepository<CnmImportReadDocument> imports,
            CapturingRepository<IidmNetworkReadDocument> networks) {
        return new SensitivityAnalysisService(
                new StandardEnvironment(),
                ObservationRegistry.NOOP,
                infrastructureUtils(imports, networks),
                new SensitivityDefaultsService(),
                "lfsa.events",
                "lfsa.sensitivity.requested",
                "lfsa-inputs");
    }

    private static CnmImportReadDocument importDocument(
            String id,
            ImportState state,
            IidmTransformationStatus iidmStatus) {
        return new CnmImportReadDocument(
                id,
                CnmServiceType.CGM,
                TimeFrame.DAY_AHEAD,
                state,
                List.of(),
                1L,
                "ready",
                iidmStatus);
    }

    private static IidmNetworkReadDocument networkDocument(String id, String importId) {
        return new IidmNetworkReadDocument(
                id,
                importId,
                List.of("file-1"),
                List.of("file-1.xml"),
                "2024-12-03",
                "04:30",
                "DAY_AHEAD",
                "TSO-XYZ",
                "XIIDM",
                "<network/>",
                List.of(),
                null,
                1L,
                1L);
    }

    private static InfrastructureUtils infrastructureUtils(
            CapturingRepository<CnmImportReadDocument> imports,
            CapturingRepository<IidmNetworkReadDocument> networks) {
        return new InfrastructureUtils() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> DocumentRepositoryService<T> documentRepository(DocumentAdapter<T> adapter) {
                if (adapter instanceof CnmImportReadDocumentAdapter) {
                    return (DocumentRepositoryService<T>) imports;
                }
                if (adapter instanceof IidmNetworkReadDocumentAdapter) {
                    return (DocumentRepositoryService<T>) networks;
                }
                return new CapturingRepository<>();
            }

            @Override
            public ObjectStorageService objectStorageService() {
                return null;
            }

            @Override
            public EventPublisherService eventPublisher() {
                return (exchange, routingKey, payload) -> {
                };
            }

            @Override
            public BusinessProcessService businessProcessService() {
                return null;
            }
        };
    }

    private static class CapturingRepository<T> implements DocumentRepositoryService<T> {
        private final List<T> documents = new ArrayList<>();

        @Override
        public void save(T document) {
            documents.add(document);
        }

        @Override
        public void saveAll(List<T> documents) {
            this.documents.addAll(documents);
        }

        @Override
        public List<T> findByField(String fieldName, Object value, int maxResults) {
            return documents.stream()
                    .filter(document -> matchesField(document, fieldName, value))
                    .limit(maxResults)
                    .toList();
        }

        @Override
        public List<T> findAll(int maxResults, DocumentSort sort) {
            return documents.stream().limit(maxResults).toList();
        }

        @Override
        public DocumentPage<T> search(DocumentSearchRequest request) {
            return new DocumentPage<>(documents, documents.size(), request.page(), request.size());
        }

        private boolean matchesField(T document, String fieldName, Object value) {
            if ("id".equals(fieldName) && document instanceof CnmImportReadDocument importDocument) {
                return importDocument.id().equals(value);
            }
            if ("id".equals(fieldName) && document instanceof IidmNetworkReadDocument networkDocument) {
                return networkDocument.id().equals(value);
            }
            return "importId".equals(fieldName)
                    && document instanceof IidmNetworkReadDocument networkDocument
                    && networkDocument.importId().equals(value);
        }
    }
}
