package eu.egm.srv.cnm.services.service;

import com.infra.InfrastructureUtils;
import com.infra.bpm.BusinessProcessService;
import com.infra.event.EventPublisherService;
import com.infra.storage.document.DocumentAdapter;
import com.infra.storage.document.DocumentPage;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSearchRequest;
import com.infra.storage.document.DocumentSort;
import com.infra.storage.object.ObjectStorageService;
import eu.egm.data.cnm.common.CnmFileProcessingRequested;
import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.ImportFailureRequest;
import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ImportFileStatusUpdateRequest;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.ImportStatus;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.TimeFrame;
import eu.egm.srv.cnm.services.domain.CnmImportDocument;
import eu.egm.srv.cnm.services.domain.CnmProfileDocument;
import eu.egm.srv.cnm.services.domain.CnmProfileDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmProfilePayloadDocument;
import eu.egm.srv.cnm.services.domain.CnmProfilePayloadDocumentAdapter;
import eu.egm.srv.cnm.services.rdf.RdfMetadataExtractor;
import io.micrometer.observation.ObservationRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class CnmImportRestServiceTest {

    @Test
    void expandsNestedZipUploadsIntoImportedRdfXmlFiles() throws Exception {
        CapturingObjectStorageService objectStorageService = new CapturingObjectStorageService();
        CapturingDocumentRepository documentRepository = new CapturingDocumentRepository();
        CapturingProfileRepository profileRepository = new CapturingProfileRepository();
        CapturingProfilePayloadRepository profilePayloadRepository = new CapturingProfilePayloadRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        CnmImportRestService service = new CnmImportRestService(
                new StandardEnvironment(),
                ObservationRegistry.NOOP,
                infrastructureUtils(
                        objectStorageService,
                        documentRepository,
                        profileRepository,
                        profilePayloadRepository,
                        eventPublisher),
                new RdfMetadataExtractor(),
                "cnm-rdf-models",
                "cnm.events",
                "cnm.file.processing.requested",
                "iidm.events",
                "iidm.profile.transform.requested");
        byte[] innerZip = zip("20241202T2330Z_1D_TSO-XYZ_SV_002.xml", rdf("StateVariables"));
        byte[] outerZip = zip(
                new ZipItem("models/CGM/20241202T2330Z_1D_TSO-XYZ_SV_002.zip", innerZip),
                new ZipItem("models/IGM/20241202T2330Z_1D_RTEFRANCE_EQ_000.xml", rdf("Equipment")),
                new ZipItem("__MACOSX/models/._ignored.xml", rdf("Ignored")));
        MockMultipartFile upload = new MockMultipartFile("file", "models.zip", "application/zip", outerZip);

        ImportStatus status = service.importModels(
                List.of(upload),
                CnmServiceType.CGM,
                TimeFrame.DAY_AHEAD,
                null,
                "Day-ahead validation model");

        assertThat(status.files()).hasSize(2);
        assertThat(objectStorageService.storedObjects).hasSize(2);
        assertThat(documentRepository.saved).hasSize(2);
        assertThat(documentRepository.saved.get(0).state()).isEqualTo(ImportState.INIT);
        assertThat(documentRepository.saved.get(1).state()).isEqualTo(ImportState.STORED);
        assertThat(status.message()).isEqualTo("Day-ahead validation model");
        assertThat(status.files())
                .extracting(file -> file.fileName())
                .containsExactly(
                        "20241202T2330Z_1D_RTEFRANCE_EQ_000.xml",
                        "20241202T2330Z_1D_TSO-XYZ_SV_002.xml");
        assertThat(status.files().get(1).businessDay()).isEqualTo("2024-12-02");
        assertThat(status.files().get(1).businessTime()).isEqualTo("23:30");
        assertThat(status.files().get(1).modelTimeFrame()).isEqualTo("1D");
        assertThat(status.files().get(1).tsoName()).isEqualTo("TSO-XYZ");
        assertThat(status.files().get(1).profileType()).isEqualTo("SV");
        assertThat(status.files().get(1).modelVersion()).isEqualTo("002");
        assertThat(status.files().get(1).profileFamily()).isEqualTo(ProfileFamily.SV);
        assertThat(status.files()).allMatch(file -> file.state() == ImportFileState.STORED);
        assertThat(profileRepository.saved).isEmpty();
        assertThat(eventPublisher.published)
                .extracting(event -> event.routingKey)
                .containsExactly("cnm.file.processing.requested", "cnm.file.processing.requested");
        assertThat(eventPublisher.processingEvents()).hasSize(2);

        ImportStatus processed = status;
        for (CnmFileProcessingRequested event : eventPublisher.processingEvents()) {
            processed = service.processFile(event);
        }

        assertThat(eventPublisher.published)
                .extracting(event -> event.routingKey)
                .contains("iidm.profile.transform.requested", "iidm.profile.transform.requested");
        assertThat(processed.state()).isEqualTo(ImportState.SUCCESS);
        assertThat(processed.message()).isEqualTo("All CNM files processed successfully");
        assertThat(processed.files()).allMatch(file -> file.state() == ImportFileState.PARSED);
        assertThat(profileRepository.saved).hasSize(2);
        CnmProfileDocument svProfile = profileRepository.saved.stream()
                .filter(profile -> "SV".equals(profile.profileType()))
                .findFirst()
                .orElseThrow();
        assertThat(svProfile.state()).isEqualTo(ImportFileState.PARSED);
        assertThat(svProfile.profileFamily()).isEqualTo(ProfileFamily.SV);
        assertThat(svProfile.tsoName()).isEqualTo("TSO-XYZ");
        assertThat(svProfile.timeFrame()).isEqualTo("1D");
        assertThat(svProfile.version()).isEqualTo("002");
        assertThat(svProfile.profileJsonType()).isEqualTo("cgmes.sv");
        assertThat(svProfile.entityCounts()).isNotEmpty();
        assertThat(profilePayloadRepository.saved).hasSize(2);
        assertThat(profilePayloadRepository.saved.stream()
                .filter(payload -> payload.id().equals(svProfile.fileId()))
                .findFirst()
                .orElseThrow()
                .profileJson()).contains("\"profileType\":\"SV\"");
        assertThat(service.profilePayload(status.importId(), svProfile.fileId())).isInstanceOf(Map.class);
        assertThat(service.profileTables(status.importId(), svProfile.fileId()).tables())
                .extracting(table -> table.tableId())
                .contains("topologyObjects", "voltages");
    }

    @Test
    void recordsClientUploadFailureAndReusesImportIdForRetry() throws Exception {
        CapturingObjectStorageService objectStorageService = new CapturingObjectStorageService();
        CapturingDocumentRepository documentRepository = new CapturingDocumentRepository();
        CapturingProfileRepository profileRepository = new CapturingProfileRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        CnmImportRestService service = new CnmImportRestService(
                new StandardEnvironment(),
                ObservationRegistry.NOOP,
                infrastructureUtils(
                        objectStorageService,
                        documentRepository,
                        profileRepository,
                        new NoopDocumentRepository<>(),
                        eventPublisher),
                new RdfMetadataExtractor(),
                "cnm-rdf-models",
                "cnm.events",
                "cnm.file.processing.requested",
                "iidm.events",
                "iidm.profile.transform.requested");
        String importId = "client-import-id";

        ImportStatus failed = service.reportFailure(new ImportFailureRequest(
                importId,
                CnmServiceType.CGM,
                TimeFrame.DAY_AHEAD,
                List.of("models.zip"),
                "Unable to import model: 413"));

        MockMultipartFile retry = new MockMultipartFile(
                "file",
                "20241202T2330Z_1D_TSO-XYZ_SV_002.xml",
                "application/xml",
                rdf("StateVariables"));
        ImportStatus completed = service.importModels(
                List.of(retry),
                CnmServiceType.CGM,
                TimeFrame.DAY_AHEAD,
                importId);

        assertThat(failed.importId()).isEqualTo(importId);
        assertThat(failed.state()).isEqualTo(ImportState.FAILED);
        assertThat(failed.files()).extracting(file -> file.fileName()).containsExactly("models.zip");
        assertThat(completed.importId()).isEqualTo(importId);
        assertThat(completed.state()).isEqualTo(ImportState.STORED);
        assertThat(documentRepository.saved)
                .extracting(CnmImportDocument::state)
                .containsExactly(ImportState.FAILED, ImportState.INIT, ImportState.STORED);
        assertThat(eventPublisher.processingEvents()).singleElement()
                .extracting(CnmFileProcessingRequested::importId)
                .isEqualTo(importId);
    }

    @Test
    void appliesDownstreamFileStatusAndRecomputesAggregateState() throws Exception {
        CapturingObjectStorageService objectStorageService = new CapturingObjectStorageService();
        CapturingDocumentRepository documentRepository = new CapturingDocumentRepository();
        CapturingProfileRepository profileRepository = new CapturingProfileRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        CnmImportRestService service = new CnmImportRestService(
                new StandardEnvironment(),
                ObservationRegistry.NOOP,
                infrastructureUtils(
                        objectStorageService,
                        documentRepository,
                        profileRepository,
                        new NoopDocumentRepository<>(),
                        eventPublisher),
                new RdfMetadataExtractor(),
                "cnm-rdf-models",
                "cnm.events",
                "cnm.file.processing.requested",
                "iidm.events",
                "iidm.profile.transform.requested");
        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "20241202T2330Z_1D_TSO-XYZ_SV_002.xml",
                "application/xml",
                rdf("StateVariables"));
        ImportStatus imported = service.importModels(List.of(upload), CnmServiceType.CGM, TimeFrame.DAY_AHEAD);
        String fileId = imported.files().get(0).fileId();

        ImportStatus initialized = service.updateFileStatus(
                imported.importId(),
                fileId,
                new ImportFileStatusUpdateRequest(ImportFileState.INIT, "Downstream work queued"));
        ImportStatus stored = service.updateFileStatus(
                imported.importId(),
                fileId,
                new ImportFileStatusUpdateRequest(ImportFileState.STORED, "Awaiting downstream parse"));
        ImportStatus parsed = service.updateFileStatus(
                imported.importId(),
                fileId,
                new ImportFileStatusUpdateRequest(ImportFileState.PARSED, "Downstream parse complete"));
        ImportStatus failed = service.updateFileStatus(
                imported.importId(),
                fileId,
                new ImportFileStatusUpdateRequest(ImportFileState.FAILED, "Downstream parse failed"));

        assertThat(initialized.state()).isEqualTo(ImportState.INIT);
        assertThat(initialized.files().get(0).state()).isEqualTo(ImportFileState.INIT);
        assertThat(stored.state()).isEqualTo(ImportState.STORED);
        assertThat(stored.files().get(0).state()).isEqualTo(ImportFileState.STORED);
        assertThat(parsed.state()).isEqualTo(ImportState.SUCCESS);
        assertThat(parsed.files().get(0).state()).isEqualTo(ImportFileState.PARSED);
        assertThat(failed.state()).isEqualTo(ImportState.FAILED);
        assertThat(failed.files().get(0).state()).isEqualTo(ImportFileState.FAILED);
        assertThat(failed.files().get(0).message()).isEqualTo("Downstream parse failed");
    }

    @Test
    void findImportNormalizesLegacyTimestampAndRestoresFilenameBusinessMetadata() {
        CapturingObjectStorageService objectStorageService = new CapturingObjectStorageService();
        CapturingDocumentRepository documentRepository = new CapturingDocumentRepository();
        CnmImportRestService service = new CnmImportRestService(
                new StandardEnvironment(),
                ObservationRegistry.NOOP,
                infrastructureUtils(objectStorageService, documentRepository),
                new RdfMetadataExtractor(),
                "cnm-rdf-models",
                "cnm.events",
                "cnm.file.processing.requested",
                "iidm.events",
                "iidm.profile.transform.requested");
        long timestamp = java.time.Instant.parse("2026-06-24T18:24:05Z").toEpochMilli();
        documentRepository.save(new CnmImportDocument(
                "legacy-import",
                CnmServiceType.CGM,
                TimeFrame.DAY_AHEAD,
                ImportState.STORED,
                List.of(new CnmImportDocument.CnmImportFileDocument(
                        "legacy-file",
                        "20241202T2330Z_1D_TSO-XYZ_SV_002.xml",
                        "legacy-import/model.xml",
                        ImportFileState.PARSED,
                        null,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        List.of(),
                        "Parsed",
                        timestamp)),
                String.valueOf(timestamp),
                "Legacy import"));

        ImportStatus restored = service.findImport("legacy-import");

        assertThat(restored.createdAt()).isEqualTo(java.time.Instant.ofEpochMilli(timestamp));
        assertThat(restored.files()).singleElement().satisfies(file -> {
            assertThat(file.uploadedAt()).isEqualTo(java.time.Instant.ofEpochMilli(timestamp));
            assertThat(file.businessDay()).isEqualTo("2024-12-02");
            assertThat(file.businessTime()).isEqualTo("23:30");
            assertThat(file.modelTimeFrame()).isEqualTo("1D");
            assertThat(file.tsoName()).isEqualTo("TSO-XYZ");
            assertThat(file.profileType()).isEqualTo("SV");
            assertThat(file.profileFamily()).isEqualTo(ProfileFamily.SV);
        });
    }

    private static InfrastructureUtils infrastructureUtils(
            ObjectStorageService objectStorageService,
            DocumentRepositoryService<CnmImportDocument> documentRepository) {
        return infrastructureUtils(objectStorageService, documentRepository, new NoopDocumentRepository<>());
    }

    private static InfrastructureUtils infrastructureUtils(
            ObjectStorageService objectStorageService,
            DocumentRepositoryService<CnmImportDocument> documentRepository,
            DocumentRepositoryService<CnmProfileDocument> profileRepository) {
        return infrastructureUtils(
                objectStorageService,
                documentRepository,
                profileRepository,
                new NoopDocumentRepository<>(),
                new CapturingEventPublisher());
    }

    private static InfrastructureUtils infrastructureUtils(
            ObjectStorageService objectStorageService,
            DocumentRepositoryService<CnmImportDocument> documentRepository,
            DocumentRepositoryService<CnmProfileDocument> profileRepository,
            DocumentRepositoryService<CnmProfilePayloadDocument> profilePayloadRepository,
            EventPublisherService eventPublisher) {
        return new InfrastructureUtils() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> DocumentRepositoryService<T> documentRepository(DocumentAdapter<T> adapter) {
                if (adapter instanceof CnmProfileDocumentAdapter) {
                    return (DocumentRepositoryService<T>) profileRepository;
                }
                if (adapter instanceof CnmProfilePayloadDocumentAdapter) {
                    return (DocumentRepositoryService<T>) profilePayloadRepository;
                }
                return (DocumentRepositoryService<T>) documentRepository;
            }

            @Override
            public ObjectStorageService objectStorageService() {
                return objectStorageService;
            }

            @Override
            public EventPublisherService eventPublisher() {
                return eventPublisher;
            }

            @Override
            public BusinessProcessService businessProcessService() {
                return null;
            }
        };
    }

    private static byte[] rdf(String profileName) {
        return ("""
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:cim="http://iec.ch/TC57/CIM100#"
                         xmlns:dcterms="http://purl.org/dc/terms/"
                         xmlns:md="http://iec.ch/TC57/61970-552/ModelDescription/1#">
                  <md:FullModel rdf:about="urn:uuid:test">
                    <dcterms:conformsTo rdf:resource="https://ap-con.cim4.eu/%s/3.0"/>
                  </md:FullModel>
                  <cim:VoltageLevel rdf:ID="VL-1">
                    <cim:IdentifiedObject.name>Voltage Level 1</cim:IdentifiedObject.name>
                  </cim:VoltageLevel>
                  <cim:SvVoltage rdf:ID="SV-1">
                    <cim:SvVoltage.v>400.0</cim:SvVoltage.v>
                    <cim:SvVoltage.angle>-1.2</cim:SvVoltage.angle>
                    <cim:SvVoltage.TopologicalNode rdf:resource="#TN-1"/>
                  </cim:SvVoltage>
                </rdf:RDF>
                """).formatted(profileName).getBytes();
    }

    private static byte[] zip(String entryName, byte[] bytes) throws IOException {
        return zip(new ZipItem(entryName, bytes));
    }

    private static byte[] zip(ZipItem... items) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(bytes)) {
            for (ZipItem item : items) {
                zipOutputStream.putNextEntry(new ZipEntry(item.name()));
                zipOutputStream.write(item.bytes());
                zipOutputStream.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private record ZipItem(String name, byte[] bytes) {
    }

    private static class CapturingObjectStorageService implements ObjectStorageService {
        private final List<String> storedObjects = new ArrayList<>();
        private final Map<String, byte[]> objectPayloads = new LinkedHashMap<>();

        @Override
        public void initializeBucket(String bucketName) {
        }

        @Override
        public synchronized void store(String bucketName, String objectName, byte[] bytes, String contentType) {
            storedObjects.add(bucketName + "/" + objectName);
            objectPayloads.put(bucketName + "/" + objectName, bytes);
        }

        @Override
        public byte[] read(String bucketName, String objectName) {
            return objectPayloads.get(bucketName + "/" + objectName);
        }
    }

    private static class CapturingEventPublisher implements EventPublisherService {
        private final List<PublishedEvent> published = new ArrayList<>();

        @Override
        public void publish(String exchange, String routingKey, Object payload) {
            published.add(new PublishedEvent(exchange, routingKey, payload));
        }

        private List<CnmFileProcessingRequested> processingEvents() {
            return published.stream()
                    .map(PublishedEvent::payload)
                    .filter(CnmFileProcessingRequested.class::isInstance)
                    .map(CnmFileProcessingRequested.class::cast)
                    .toList();
        }
    }

    private record PublishedEvent(String exchange, String routingKey, Object payload) {
    }

    private static class CapturingDocumentRepository implements DocumentRepositoryService<CnmImportDocument> {
        private final List<CnmImportDocument> saved = new ArrayList<>();

        @Override
        public void save(CnmImportDocument document) {
            saved.add(document);
        }

        @Override
        public void saveAll(List<CnmImportDocument> documents) {
            saved.addAll(documents);
        }

        @Override
        public List<CnmImportDocument> findByField(String fieldName, Object value, int maxResults) {
            return saved.stream()
                    .filter(document -> "id".equals(fieldName) && document.id().equals(value))
                    .reduce((first, second) -> second)
                    .stream()
                    .toList();
        }

        @Override
        public List<CnmImportDocument> findAll(int maxResults, DocumentSort sort) {
            return List.of();
        }

        @Override
        public DocumentPage<CnmImportDocument> search(DocumentSearchRequest request) {
            return new DocumentPage<>(List.of(), 0, 0, 0);
        }
    }

    private static class CapturingProfileRepository implements DocumentRepositoryService<CnmProfileDocument> {
        private final List<CnmProfileDocument> saved = new ArrayList<>();

        @Override
        public void save(CnmProfileDocument document) {
            saved.removeIf(current -> current.id().equals(document.id()));
            saved.add(document);
        }

        @Override
        public void saveAll(List<CnmProfileDocument> documents) {
            documents.forEach(this::save);
        }

        @Override
        public List<CnmProfileDocument> findByField(String fieldName, Object value, int maxResults) {
            return saved.stream()
                    .filter(document -> "id".equals(fieldName) && document.id().equals(value))
                    .limit(maxResults)
                    .toList();
        }

        @Override
        public List<CnmProfileDocument> findAll(int maxResults, DocumentSort sort) {
            return saved.stream().limit(maxResults).toList();
        }

        @Override
        public DocumentPage<CnmProfileDocument> search(DocumentSearchRequest request) {
            return new DocumentPage<>(saved, saved.size(), request.page(), request.size());
        }
    }

    private static class CapturingProfilePayloadRepository implements DocumentRepositoryService<CnmProfilePayloadDocument> {
        private final List<CnmProfilePayloadDocument> saved = new ArrayList<>();

        @Override
        public void save(CnmProfilePayloadDocument document) {
            saved.removeIf(current -> current.id().equals(document.id()));
            saved.add(document);
        }

        @Override
        public void saveAll(List<CnmProfilePayloadDocument> documents) {
            documents.forEach(this::save);
        }

        @Override
        public List<CnmProfilePayloadDocument> findByField(String fieldName, Object value, int maxResults) {
            return saved.stream()
                    .filter(document -> "id".equals(fieldName) && document.id().equals(value))
                    .limit(maxResults)
                    .toList();
        }

        @Override
        public List<CnmProfilePayloadDocument> findAll(int maxResults, DocumentSort sort) {
            return saved.stream().limit(maxResults).toList();
        }

        @Override
        public DocumentPage<CnmProfilePayloadDocument> search(DocumentSearchRequest request) {
            return new DocumentPage<>(saved, saved.size(), request.page(), request.size());
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
