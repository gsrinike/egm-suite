package eu.egm.srv.common.lfsa.service;

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
import eu.egm.data.common.lfsa.common.CommonPage;
import eu.egm.data.common.lfsa.common.SecurityAnalysisImportCandidate;
import eu.egm.srv.common.lfsa.config.LfSaDefaultsService;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocument;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocumentAdapter;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class LfSaServiceTest {
    @Test
    void defaultSearchReturnsLatestEligibleImports() {
        CapturingImportRepository importRepository = new CapturingImportRepository();
        importRepository.save(importDocument("old-ready", TimeFrame.DAY_AHEAD, ImportState.SUCCESS, IidmTransformationStatus.DONE, 1L));
        importRepository.save(importDocument("not-iidm-ready", TimeFrame.DAY_AHEAD, ImportState.SUCCESS, IidmTransformationStatus.STARTED, 2L));
        importRepository.save(importDocument("not-import-ready", TimeFrame.DAY_AHEAD, ImportState.IN_PROGRESS, IidmTransformationStatus.DONE, 3L));
        importRepository.save(importDocument("new-ready", TimeFrame.ID, ImportState.SUCCESS, IidmTransformationStatus.DONE, 4L));
        LfSaService service = service(importRepository);

        CommonPage<SecurityAnalysisImportCandidate> page = service.searchSuccessfulImports("CGM", "", "", 0, 100);

        assertThat(page.items())
                .extracting(SecurityAnalysisImportCandidate::importId)
                .containsExactly("new-ready", "old-ready");
    }

    @Test
    void searchAcceptsLegacyTimeFrameAliases() {
        CapturingImportRepository importRepository = new CapturingImportRepository();
        importRepository.save(importDocument("day-ahead", TimeFrame.DAY_AHEAD, ImportState.SUCCESS, IidmTransformationStatus.DONE, 1L));
        importRepository.save(importDocument("intraday", TimeFrame.ID, ImportState.SUCCESS, IidmTransformationStatus.DONE, 2L));
        importRepository.save(importDocument("two-days", TimeFrame.TWO_DAYS_AHEAD, ImportState.SUCCESS, IidmTransformationStatus.DONE, 3L));
        LfSaService service = service(importRepository);

        assertThat(service.searchSuccessfulImports("CGM", "DAY", "", 0, 100).items())
                .extracting(SecurityAnalysisImportCandidate::importId)
                .containsExactly("day-ahead");
        assertThat(service.searchSuccessfulImports("CGM", "INTRA", "", 0, 100).items())
                .extracting(SecurityAnalysisImportCandidate::importId)
                .containsExactly("intraday");
        assertThat(service.searchSuccessfulImports("CGM", "2D", "", 0, 100).items())
                .extracting(SecurityAnalysisImportCandidate::importId)
                .containsExactly("two-days");
    }

    @Test
    void dateSearchMatchesAnyImportFileAndDisplaysNonBoundaryBusinessDay() {
        CapturingImportRepository importRepository = new CapturingImportRepository();
        importRepository.save(importDocumentWithFiles(
                "with-boundary",
                TimeFrame.DAY_AHEAD,
                ImportState.SUCCESS,
                IidmTransformationStatus.DONE,
                1L,
                List.of(
                        importFile("boundary", "2024-11-22", TimeFrame.DAY_AHEAD, "EQBD"),
                        importFile("model", "2024-12-03", TimeFrame.DAY_AHEAD, "EQ"))));
        LfSaService service = service(importRepository);

        CommonPage<SecurityAnalysisImportCandidate> page =
                service.searchSuccessfulImports("CGM", "DAY_AHEAD", "2024-12-03", 0, 100);

        assertThat(page.items()).singleElement().satisfies(candidate -> {
            assertThat(candidate.importId()).isEqualTo("with-boundary");
            assertThat(candidate.businessDay()).isEqualTo("2024-12-03");
        });
    }

    private static LfSaService service(CapturingImportRepository importRepository) {
        return new LfSaService(
                new StandardEnvironment(),
                ObservationRegistry.NOOP,
                infrastructureUtils(importRepository),
                new LfSaDefaultsService(),
                "lfsa.events",
                "lfsa.security-analysis.requested");
    }

    private static CnmImportReadDocument importDocument(
            String id,
            TimeFrame timeFrame,
            ImportState state,
            IidmTransformationStatus iidmTransformationStatus,
            long createdAt) {
        return importDocumentWithFiles(
                id,
                timeFrame,
                state,
                iidmTransformationStatus,
                createdAt,
                List.of(importFile(id + "-file", "2024-12-03", timeFrame, "EQ")));
    }

    private static CnmImportReadDocument importDocumentWithFiles(
            String id,
            TimeFrame timeFrame,
            ImportState state,
            IidmTransformationStatus iidmTransformationStatus,
            long createdAt,
            List<CnmImportReadDocument.CnmImportFileReadDocument> files) {
        return new CnmImportReadDocument(
                id,
                CnmServiceType.CGM,
                timeFrame,
                state,
                files,
                createdAt,
                "ready",
                iidmTransformationStatus);
    }

    private static CnmImportReadDocument.CnmImportFileReadDocument importFile(
            String id,
            String businessDay,
            TimeFrame timeFrame,
            String profileType) {
        return new CnmImportReadDocument.CnmImportFileReadDocument(
                id,
                id + ".xml",
                id + "/object",
                null,
                null,
                businessDay,
                "04:30",
                timeFrame.name(),
                "TSO-XYZ",
                profileType,
                "001",
                null,
                "",
                1L);
    }

    private static InfrastructureUtils infrastructureUtils(CapturingImportRepository importRepository) {
        return new InfrastructureUtils() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> DocumentRepositoryService<T> documentRepository(DocumentAdapter<T> adapter) {
                if (adapter instanceof CnmImportReadDocumentAdapter) {
                    return (DocumentRepositoryService<T>) importRepository;
                }
                return new NoopDocumentRepository<>();
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

    private static class CapturingImportRepository implements DocumentRepositoryService<CnmImportReadDocument> {
        private final List<CnmImportReadDocument> documents = new ArrayList<>();

        @Override
        public void save(CnmImportReadDocument document) {
            documents.add(document);
        }

        @Override
        public void saveAll(List<CnmImportReadDocument> documents) {
            this.documents.addAll(documents);
        }

        @Override
        public List<CnmImportReadDocument> findByField(String fieldName, Object value, int maxResults) {
            return documents.stream()
                    .filter(document -> "id".equals(fieldName) && document.id().equals(value))
                    .limit(maxResults)
                    .toList();
        }

        @Override
        public List<CnmImportReadDocument> findAll(int maxResults, DocumentSort sort) {
            return documents.stream()
                    .sorted((left, right) -> Long.compare(number(right.createdAt()), number(left.createdAt())))
                    .limit(maxResults)
                    .toList();
        }

        @Override
        public DocumentPage<CnmImportReadDocument> search(DocumentSearchRequest request) {
            return new DocumentPage<>(documents, documents.size(), request.page(), request.size());
        }

        private long number(Object value) {
            return value instanceof Number number ? number.longValue() : 0L;
        }
    }

    private static class NoopDocumentRepository<T> implements DocumentRepositoryService<T> {
        @Override
        public void save(T document) {
        }

        @Override
        public void saveAll(List<T> documents) {
        }

        @Override
        public List<T> findByField(String fieldName, Object value, int maxResults) {
            return List.of();
        }

        @Override
        public List<T> findAll(int maxResults, DocumentSort sort) {
            return List.of();
        }

        @Override
        public DocumentPage<T> search(DocumentSearchRequest request) {
            return new DocumentPage<>(List.of(), 0, request.page(), request.size());
        }
    }
}
