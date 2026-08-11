package eu.egm.srv.cnm.services.service;

import com.infra.InfrastructureUtils;
import com.infra.event.EventPublisherService;
import com.infra.storage.document.DocumentFilter;
import com.infra.storage.document.DocumentPage;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSearchRequest;
import com.infra.storage.document.DocumentSort;
import com.infra.storage.object.ObjectStorageService;
import com.utils.restservice.RestServiceSupport;
import eu.egm.data.cnm.cgmes.CgmesProfileKind;
import eu.egm.data.cnm.common.CnmFileProcessingRequested;
import eu.egm.data.cnm.common.CnmPage;
import eu.egm.data.cnm.common.CnmProfileMetadata;
import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.CnmSnapshotAssemblyRequested;
import eu.egm.data.cnm.common.CnmSnapshotMetadata;
import eu.egm.data.cnm.common.CnmSnapshotState;
import eu.egm.data.cnm.common.CnmTransformInitializationRequested;
import eu.egm.data.cnm.common.DynamicTableBundle;
import eu.egm.data.cnm.common.DynamicTableColumn;
import eu.egm.data.cnm.common.DynamicTableDefinition;
import eu.egm.data.cnm.common.DynamicTableRow;
import eu.egm.data.cnm.common.ImportFailureRequest;
import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ImportFileStatus;
import eu.egm.data.cnm.common.IidmTransformationStatus;
import eu.egm.data.cnm.common.ImportFileStatusUpdateRequest;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.ImportStatus;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.TimeFrame;
import eu.egm.data.iidm.common.CgmesIidmImportOptions;
import eu.egm.data.iidm.common.CgmesIidmSourceFile;
import eu.egm.data.iidm.common.IidmProfileTransformCompleted;
import eu.egm.data.iidm.common.IidmProfileTransformFailed;
import eu.egm.data.iidm.common.IidmProfileTransformRequested;
import eu.egm.data.iidm.common.IidmProfileTransformStarted;
import eu.egm.mapping.JacksonJsonMappingService;
import eu.egm.mapping.JsonMappingService;
import eu.egm.srv.cnm.services.domain.CnmImportDocument;
import eu.egm.srv.cnm.services.domain.CnmImportDocument.CnmImportFileDocument;
import eu.egm.srv.cnm.services.domain.CnmImportDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmMridIndexDocument;
import eu.egm.srv.cnm.services.domain.CnmMridIndexDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmNetworkSnapshotDocument;
import eu.egm.srv.cnm.services.domain.CnmNetworkSnapshotDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmNetworkSnapshotPayloadDocument;
import eu.egm.srv.cnm.services.domain.CnmNetworkSnapshotPayloadDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmProfileDocument;
import eu.egm.srv.cnm.services.domain.CnmProfileDocument.CnmEntityCountDocument;
import eu.egm.srv.cnm.services.domain.CnmProfileDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmProfileFragmentChunkDocument;
import eu.egm.srv.cnm.services.domain.CnmProfileFragmentChunkDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmProfileFragmentDocument;
import eu.egm.srv.cnm.services.domain.CnmProfileFragmentDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmProfilePayloadChunkDocument;
import eu.egm.srv.cnm.services.domain.CnmProfilePayloadChunkDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmProfilePayloadDocument;
import eu.egm.srv.cnm.services.domain.CnmProfilePayloadDocumentAdapter;
import eu.egm.srv.cnm.services.domain.IidmProfileTransformReadDocument;
import eu.egm.srv.cnm.services.domain.IidmProfileTransformReadDocumentAdapter;
import eu.egm.srv.cnm.services.service.CnmModelFileNameParser.CnmModelFileName;
import eu.egm.srv.cnm.services.rdf.CgmSnapshotAssembler;
import eu.egm.srv.cnm.services.rdf.ProfileProcessingContext;
import eu.egm.srv.cnm.services.rdf.RdfMetadata;
import eu.egm.srv.cnm.services.rdf.RdfMetadataExtractor;
import eu.egm.data.cnm.rdf.ProfileFragment;
import eu.egm.data.cnm.snapshot.CgmNetworkSnapshot;
import io.micrometer.observation.ObservationRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CnmImportRestService extends RestServiceSupport {
    private static final int PROFILE_JSON_CHUNK_SIZE = 1_000_000;
    private static final int PROFILE_JSON_CHUNK_BATCH_SIZE = 10;
    private static final int SNAPSHOT_PAYLOAD_SECTION_TARGET_SIZE = 500_000;
    private static final int MAX_PROFILE_PAGE_SIZE = 50;
    private static final List<String> PROFILE_LIST_EXCLUDED_FIELDS = List.of("profileJson", "profileJsonChunks");
    private static final Set<CgmesProfileKind> REQUIRED_IIDM_PROFILE_KINDS = Set.of(
            CgmesProfileKind.EQUIPMENT,
            CgmesProfileKind.TOPOLOGY,
            CgmesProfileKind.STEADY_STATE_HYPOTHESIS,
            CgmesProfileKind.STATE_VARIABLES);

    private final ObjectStorageService objectStorageService;
    private final DocumentRepositoryService<CnmImportDocument> documentRepository;
    private final DocumentRepositoryService<CnmProfileDocument> profileRepository;
    private final DocumentRepositoryService<CnmProfilePayloadDocument> profilePayloadRepository;
    private final DocumentRepositoryService<CnmProfileFragmentDocument> profileFragmentRepository;
    private final DocumentRepositoryService<CnmProfilePayloadChunkDocument> profilePayloadChunkRepository;
    private final DocumentRepositoryService<CnmProfileFragmentChunkDocument> profileFragmentChunkRepository;
    private final DocumentRepositoryService<CnmMridIndexDocument> mridIndexRepository;
    private final DocumentRepositoryService<CnmNetworkSnapshotDocument> networkSnapshotRepository;
    private final DocumentRepositoryService<CnmNetworkSnapshotPayloadDocument> networkSnapshotPayloadRepository;
    private final DocumentRepositoryService<IidmProfileTransformReadDocument> iidmTransformRepository;
    private final EventPublisherService eventPublisher;
    private final JsonMappingService jsonMappingService;
    private final RdfMetadataExtractor metadataExtractor;
    private final CgmSnapshotAssembler snapshotAssembler = new CgmSnapshotAssembler();
    private final boolean mridIndexEnabled;
    private final long slowMetadataProcessingThresholdMs;
    private final String rawBucket;
    private final String eventExchange;
    private final String transformInitializationRoutingKey;
    private final String fileProcessingRoutingKey;
    private final String snapshotAssemblyRoutingKey;
    private final String iidmTransformExchange;
    private final String iidmTransformRoutingKey;
    private final ConcurrentMap<String, Object> snapshotLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> importStatusLocks = new ConcurrentHashMap<>();
    private final String profilePayloadBucket;
    private final String profileFragmentBucket;
    private final String networkSnapshotPayloadBucket;

    public CnmImportRestService(
            Environment environment,
            ObservationRegistry observationRegistry,
            InfrastructureUtils infrastructureUtils,
            RdfMetadataExtractor metadataExtractor,
            @Value("${cnm.import.raw-bucket:cnm-rdf-models}") String rawBucket,
            @Value("${cnm.import.event.exchange:cnm.events}") String eventExchange,
            @Value("${cnm.import.event.file-processing-routing-key:cnm.file.processing.requested}") String fileProcessingRoutingKey,
            @Value("${cnm.import.event.snapshot-assembly-routing-key:cnm.snapshot.assembly.requested}") String snapshotAssemblyRoutingKey,
            @Value("${cnm.import.event.iidm-transform-exchange:iidm.events}") String iidmTransformExchange,
            @Value("${cnm.import.event.iidm-transform-routing-key:iidm.profile.transform.requested}") String iidmTransformRoutingKey,
            @Value("${cnm.import.profile-payload-bucket:cnm-profile-payloads}") String profilePayloadBucket,
            @Value("${cnm.import.profile-fragment-bucket:cnm-profile-fragments}") String profileFragmentBucket,
            @Value("${cnm.import.network-snapshot-payload-bucket:cnm-network-snapshot-payloads}") String networkSnapshotPayloadBucket) {
        this(
                environment,
                observationRegistry,
                infrastructureUtils,
                new JacksonJsonMappingService(),
                metadataExtractor,
                rawBucket,
                eventExchange,
                fileProcessingRoutingKey,
                snapshotAssemblyRoutingKey,
                iidmTransformExchange,
                iidmTransformRoutingKey,
                profilePayloadBucket,
                profileFragmentBucket,
                networkSnapshotPayloadBucket);
    }

    @Autowired
    public CnmImportRestService(
            Environment environment,
            ObservationRegistry observationRegistry,
            InfrastructureUtils infrastructureUtils,
            JsonMappingService jsonMappingService,
            RdfMetadataExtractor metadataExtractor,
            @Value("${cnm.import.raw-bucket:cnm-rdf-models}") String rawBucket,
            @Value("${cnm.import.event.exchange:cnm.events}") String eventExchange,
            @Value("${cnm.import.event.file-processing-routing-key:cnm.file.processing.requested}") String fileProcessingRoutingKey,
            @Value("${cnm.import.event.snapshot-assembly-routing-key:cnm.snapshot.assembly.requested}") String snapshotAssemblyRoutingKey,
            @Value("${cnm.import.event.iidm-transform-exchange:iidm.events}") String iidmTransformExchange,
            @Value("${cnm.import.event.iidm-transform-routing-key:iidm.profile.transform.requested}") String iidmTransformRoutingKey,
            @Value("${cnm.import.profile-payload-bucket:cnm-profile-payloads}") String profilePayloadBucket,
            @Value("${cnm.import.profile-fragment-bucket:cnm-profile-fragments}") String profileFragmentBucket,
            @Value("${cnm.import.network-snapshot-payload-bucket:cnm-network-snapshot-payloads}") String networkSnapshotPayloadBucket) {
        super(environment, observationRegistry);
        this.objectStorageService = infrastructureUtils.objectStorageService();
        this.documentRepository = infrastructureUtils.documentRepository(new CnmImportDocumentAdapter());
        this.profileRepository = infrastructureUtils.documentRepository(new CnmProfileDocumentAdapter());
        this.profilePayloadRepository = infrastructureUtils.documentRepository(new CnmProfilePayloadDocumentAdapter());
        this.profileFragmentRepository = infrastructureUtils.documentRepository(new CnmProfileFragmentDocumentAdapter());
        this.profilePayloadChunkRepository =
                infrastructureUtils.documentRepository(new CnmProfilePayloadChunkDocumentAdapter());
        this.profileFragmentChunkRepository =
                infrastructureUtils.documentRepository(new CnmProfileFragmentChunkDocumentAdapter());
        this.mridIndexRepository = infrastructureUtils.documentRepository(new CnmMridIndexDocumentAdapter());
        this.networkSnapshotRepository = infrastructureUtils.documentRepository(new CnmNetworkSnapshotDocumentAdapter());
        this.networkSnapshotPayloadRepository = infrastructureUtils.documentRepository(new CnmNetworkSnapshotPayloadDocumentAdapter());
        this.iidmTransformRepository = infrastructureUtils.documentRepository(new IidmProfileTransformReadDocumentAdapter());
        this.eventPublisher = infrastructureUtils.eventPublisher();
        this.jsonMappingService = jsonMappingService;
        this.metadataExtractor = metadataExtractor;
        this.mridIndexEnabled = environment.getProperty("cnm.import.metadata.mrid-index.enabled", Boolean.class, true);
        this.slowMetadataProcessingThresholdMs =
                environment.getProperty("cnm.import.metadata.slow-threshold-ms", Long.class, 5_000L);
        this.rawBucket = rawBucket;
        this.profilePayloadBucket = profilePayloadBucket;
        this.profileFragmentBucket = profileFragmentBucket;
        this.networkSnapshotPayloadBucket = networkSnapshotPayloadBucket;
        this.eventExchange = eventExchange;
        this.transformInitializationRoutingKey = environment.getProperty(
                "cnm.import.event.transform-initialization-routing-key",
                "cnm.transform.initialization.requested");
        this.fileProcessingRoutingKey = fileProcessingRoutingKey;
        this.snapshotAssemblyRoutingKey = snapshotAssemblyRoutingKey;
        this.iidmTransformExchange = iidmTransformExchange;
        this.iidmTransformRoutingKey = iidmTransformRoutingKey;
    }

    public ImportStatus importModels(Collection<MultipartFile> uploads, CnmServiceType serviceType, TimeFrame timeFrame)
            throws IOException {
        return importModels(uploads, serviceType, timeFrame, null, null);
    }

    public ImportStatus importModels(
            Collection<MultipartFile> uploads,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            String requestedImportId) throws IOException {
        return importModels(uploads, serviceType, timeFrame, requestedImportId, null);
    }

    public ImportStatus importModels(
            Collection<MultipartFile> uploads,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            String requestedImportId,
            String importMessage) throws IOException {
        if (uploads == null || uploads.isEmpty()) {
            throw new IllegalArgumentException("At least one RDF/XML or ZIP file is required");
        }
        String importId = resolveImportId(requestedImportId);
        Instant createdAt = Instant.now();
        List<String> sourceFileNames = uploads.stream()
                .filter(upload -> upload != null && !upload.isEmpty())
                .map(MultipartFile::getOriginalFilename)
                .map(name -> name == null || name.isBlank() ? "upload" : name)
                .toList();
        documentRepository.save(statusDocument(
                importId,
                serviceType,
                timeFrame,
                ImportState.INIT,
                sourceFileNames,
                createdAt,
                statusMessage(importMessage, "Upload received; model extraction is starting")));

        List<RdfPayload> payloads = new ArrayList<>();
        try {
            for (MultipartFile upload : uploads) {
                if (upload != null && !upload.isEmpty()) {
                    collectRdfPayloads(upload.getOriginalFilename(), upload.getInputStream(), payloads);
                }
            }
        } catch (Exception exception) {
            return saveFailedImport(importId, serviceType, timeFrame, sourceFileNames, createdAt, message(exception));
        }
        if (payloads.isEmpty()) {
            return saveFailedImport(
                    importId,
                    serviceType,
                    timeFrame,
                    sourceFileNames,
                    createdAt,
                    "No RDF/XML payloads found in uploaded files");
        }

        int threadCount = Math.max(1, payloads.size());
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        try {
            List<CnmImportFileDocument> files = payloads.stream()
                    .map(payload -> CompletableFuture.supplyAsync(
                            () -> storePayload(importId, payload, timeFrame),
                            executorService))
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparing(CnmImportFileDocument::fileName))
                    .toList();
            ImportState state = files.stream().anyMatch(file -> file.state() == ImportFileState.FAILED)
                    ? ImportState.FAILED
                    : ImportState.STARTED;
            String message = statusMessage(
                    importMessage,
                    "Stored " + files.size() + " RDF/XML model file" + (files.size() == 1 ? "" : "s")
                            + "; metadata processing queued");
            CnmImportDocument document = new CnmImportDocument(
                    importId,
                    serviceType,
                    timeFrame,
                    state,
                    files,
                    createdAt.toEpochMilli(),
                    message);
            documentRepository.save(document);
            CnmImportDocument queuedDocument = publishTransformInitializationEvent(document);
            if (queuedDocument != document) {
                documentRepository.save(queuedDocument);
                document = queuedDocument;
            }
            logger.info("Imported CNM upload {} with {} RDF/XML payloads", importId, files.size());
            return toStatus(document);
        } finally {
            executorService.shutdown();
        }
    }

    public ImportStatus reportFailure(ImportFailureRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Import failure details are required");
        }
        String importId = resolveImportId(request.importId());
        return saveFailedImport(
                importId,
                request.serviceType(),
                request.timeFrame(),
                request.fileNames(),
                Instant.now(),
                request.message());
    }

    private ImportStatus saveFailedImport(
            String importId,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            List<String> fileNames,
            Instant createdAt,
            String failureMessage) {
        String safeMessage = failureMessage == null || failureMessage.isBlank()
                ? "Unable to import model"
                : failureMessage;
        CnmImportDocument document = statusDocument(
                importId,
                serviceType,
                timeFrame,
                ImportState.FAILED,
                fileNames,
                createdAt,
                safeMessage);
        documentRepository.save(document);
        logger.warn("CNM import {} failed: {}", importId, safeMessage);
        return toStatus(document);
    }

    private CnmImportDocument statusDocument(
            String importId,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            ImportState state,
            List<String> fileNames,
            Instant createdAt,
            String message) {
        List<CnmImportFileDocument> files = (fileNames == null ? List.<String>of() : fileNames).stream()
                .map(fileName -> statusFile(fileName, state, timeFrame, message, createdAt))
                .toList();
        return new CnmImportDocument(
                importId,
                serviceType,
                timeFrame,
                state,
                files,
                createdAt.toEpochMilli(),
                message);
    }

    private CnmImportFileDocument statusFile(
            String fileName,
            ImportState state,
            TimeFrame timeFrame,
            String message,
            Instant createdAt) {
        String safeFileName = fileName == null || fileName.isBlank() ? "upload" : fileName;
        CnmModelFileName modelFileName = parseModelFileName(safeFileName, timeFrame);
        return new CnmImportFileDocument(
                UUID.randomUUID().toString(),
                safeFileName,
                "",
                toFileState(state),
                modelFileName.profileFamily(),
                modelFileName.businessDay(),
                modelFileName.businessTime(),
                modelFileName.timeFrame(),
                modelFileName.tsoName(),
                modelFileName.profileType(),
                modelFileName.version(),
                List.of(),
                message,
                createdAt.toEpochMilli());
    }

    private ImportFileState toFileState(ImportState state) {
        return switch (state) {
            case INIT -> ImportFileState.INIT;
            case STARTED, IN_PROGRESS -> ImportFileState.STORED;
            case SUCCESS -> ImportFileState.PARSED;
            case FAILED -> ImportFileState.FAILED;
        };
    }

    private String resolveImportId(String requestedImportId) {
        if (requestedImportId == null || requestedImportId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        if (!requestedImportId.matches("[A-Za-z0-9-]{1,100}")) {
            throw new IllegalArgumentException("Invalid import ID");
        }
        return requestedImportId;
    }

    private String message(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private String statusMessage(String requestedMessage, String fallback) {
        return requestedMessage == null || requestedMessage.isBlank() ? fallback : requestedMessage.trim();
    }

    private CnmImportFileDocument storePayload(String importId, RdfPayload payload, TimeFrame timeFrame) {
        Instant uploadedAt = Instant.now();
        CnmModelFileName modelFileName = parseModelFileName(payload.fileName(), timeFrame);
        String fileId = UUID.randomUUID().toString();
        String objectId = importId + "/" + sanitize(payload.relativePath());
        try {
            objectStorageService.store(rawBucket, objectId, payload.bytes(), contentType(payload.fileName()));
            return new CnmImportFileDocument(
                    fileId,
                    payload.fileName(),
                    objectId,
                    ImportFileState.STORED,
                    modelFileName.profileFamily(),
                    modelFileName.businessDay(),
                    modelFileName.businessTime(),
                    modelFileName.timeFrame(),
                    modelFileName.tsoName(),
                    modelFileName.profileType(),
                    modelFileName.version(),
                    List.of(),
                    "Raw model stored; metadata processing queued",
                    uploadedAt.toEpochMilli());
        } catch (Exception exception) {
            logger.warn("Unable to import CNM RDF/XML payload {}", payload.relativePath(), exception);
            return new CnmImportFileDocument(
                    fileId,
                    payload.fileName(),
                    objectId,
                    ImportFileState.FAILED,
                    modelFileName.profileFamily(),
                    modelFileName.businessDay(),
                    modelFileName.businessTime(),
                    modelFileName.timeFrame(),
                    modelFileName.tsoName(),
                    modelFileName.profileType(),
                    modelFileName.version(),
                    List.of(),
                    exception.getMessage(),
                    uploadedAt.toEpochMilli());
        }
    }

    private CnmImportDocument publishTransformInitializationEvent(CnmImportDocument document) {
        if (document.state() == ImportState.FAILED) {
            return document;
        }
        try {
            eventPublisher.publish(
                    eventExchange,
                    transformInitializationRoutingKey,
                    new CnmTransformInitializationRequested(
                            document.id(),
                            document.serviceType(),
                            document.timeFrame(),
                            0,
                            Instant.now()));
            return document;
        } catch (RuntimeException exception) {
            logger.warn("Unable to publish CNM transform-initialization event for {}", document.id(), exception);
            List<CnmImportFileDocument> failedFiles = document.files().stream()
                    .map(file -> file.state() == ImportFileState.STORED
                            ? withStatus(
                                    file,
                                    ImportFileState.FAILED,
                                    "Unable to queue transform initialization: " + message(exception))
                            : file)
                    .toList();
            return new CnmImportDocument(
                    document.id(),
                    document.serviceType(),
                    document.timeFrame(),
                    aggregateState(failedFiles),
                    failedFiles,
                    document.createdAt(),
                    "Unable to queue transform initialization",
                    document.iidmTransformationStatus());
        }
    }

    public ImportStatus initializeTransform(CnmTransformInitializationRequested event) {
        if (event == null) {
            throw new IllegalArgumentException("Transform-initialization event is required");
        }
        CnmImportDocument current = findImportDocument(event.importId());
        if (current.state() == ImportState.FAILED
                || current.state() == ImportState.SUCCESS) {
            return toStatus(current);
        }
        CnmImportDocument initialized = new CnmImportDocument(
                current.id(),
                current.serviceType(),
                current.timeFrame(),
                ImportState.IN_PROGRESS,
                current.files(),
                current.createdAt(),
                current.message(),
                current.iidmTransformationStatus());
        CnmImportDocument queuedDocument = publishProcessingEvents(initialized);
        documentRepository.save(queuedDocument);
        return toStatus(queuedDocument);
    }

    private CnmImportDocument publishProcessingEvents(CnmImportDocument document) {
        List<CnmImportFileDocument> files = document.files();
        List<CnmImportFileDocument> updatedFiles = new ArrayList<>(files.size());
        boolean changed = false;
        for (CnmImportFileDocument file : files.stream()
                .sorted(Comparator
                        .comparingInt((CnmImportFileDocument item) -> CnmFileProcessingPriority.profilePriority(item.fileName()))
                        .thenComparing(item -> instant(item.uploadedAt())))
                .toList()) {
            if (file.state() != ImportFileState.STORED) {
                updatedFiles.add(file);
                continue;
            }
            try {
                publishFileProcessingRequested(document, file, 0);
                updatedFiles.add(file);
            } catch (RuntimeException exception) {
                changed = true;
                logger.warn("Unable to publish CNM file-processing event for {}", file.fileId(), exception);
                updatedFiles.add(withStatus(
                        file,
                        ImportFileState.FAILED,
                        "Unable to queue metadata processing: " + message(exception)));
            }
        }
        if (!changed) {
            return document;
        }
        return new CnmImportDocument(
                document.id(),
                document.serviceType(),
                document.timeFrame(),
                changed ? aggregateState(updatedFiles) : ImportState.IN_PROGRESS,
                updatedFiles,
                document.createdAt(),
                "One or more files could not be queued for metadata processing",
                document.iidmTransformationStatus());
    }

    private void publishFileProcessingRequested(
            CnmImportDocument document,
            CnmImportFileDocument file,
            int retryCount) {
        eventPublisher.publish(
                eventExchange,
                fileProcessingRoutingKey,
                new CnmFileProcessingRequested(
                        document.id(),
                        file.fileId(),
                        file.objectId(),
                        file.fileName(),
                        document.serviceType(),
                        document.timeFrame(),
                        modelGroupKey(file),
                        isBoundaryProfile(file),
                        retryCount,
                        Instant.now()));
    }

    private void collectRdfPayloads(String sourceName, byte[] payload, List<RdfPayload> payloads) throws IOException {
        String safeSourceName = sourceName == null || sourceName.isBlank() ? "upload" : sourceName;
        collectRdfPayloads(safeSourceName, safeSourceName, payload, payloads);
    }

    private void collectRdfPayloads(String sourceName, InputStream input, List<RdfPayload> payloads) throws IOException {
        String safeSourceName = sourceName == null || sourceName.isBlank() ? "upload" : sourceName;
        if (lower(safeSourceName).endsWith(".zip")) {
            try (ZipInputStream zipInputStream = new ZipInputStream(input)) {
                collectZipEntries(safeSourceName, zipInputStream, payloads);
            }
            return;
        }
        collectRdfPayloads(safeSourceName, safeSourceName, input.readAllBytes(), payloads);
    }

    private void collectRdfPayloads(String relativePath, String fileName, byte[] payload, List<RdfPayload> payloads)
            throws IOException {
        if (shouldIgnore(relativePath)) {
            return;
        }
        if (isZip(fileName, payload)) {
            try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(payload))) {
                collectZipEntries(relativePath, zipInputStream, payloads);
            }
            return;
        }
        if (isRdfXml(fileName, payload)) {
            payloads.add(new RdfPayload(relativePath, baseName(fileName), payload));
        }
    }

    private void collectZipEntries(String relativePath, ZipInputStream zipInputStream, List<RdfPayload> payloads)
            throws IOException {
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                byte[] entryBytes = zipInputStream.readAllBytes();
                String entryName = entry.getName();
                collectRdfPayloads(relativePath + "/" + entryName, baseName(entryName), entryBytes, payloads);
            }
            zipInputStream.closeEntry();
        }
    }

    private boolean isZip(String fileName, byte[] payload) {
        return lower(fileName).endsWith(".zip")
                || (payload.length >= 4 && payload[0] == 'P' && payload[1] == 'K' && payload[2] == 3 && payload[3] == 4);
    }

    private boolean isRdfXml(String fileName, byte[] payload) {
        String lowerName = lower(fileName);
        return lowerName.endsWith(".xml")
                || lowerName.endsWith(".rdf")
                || lowerName.endsWith(".idm")
                || lowerName.endsWith(".owl")
                || startsWithXml(payload);
    }

    private boolean startsWithXml(byte[] payload) {
        int index = 0;
        while (index < payload.length && Character.isWhitespace((char) payload[index])) {
            index++;
        }
        return index < payload.length && payload[index] == '<';
    }

    private boolean shouldIgnore(String path) {
        String normalized = path.replace('\\', '/');
        String name = baseName(normalized);
        return normalized.contains("__MACOSX/")
                || name.startsWith("._")
                || ".DS_Store".equals(name);
    }

    private CnmModelFileName parseModelFileName(String fileName) {
        return CnmModelFileNameParser.parse(fileName);
    }

    private CnmModelFileName parseModelFileName(String fileName, TimeFrame timeFrame) {
        return CnmModelFileNameParser.parse(fileName, timeFrame);
    }

    private String contentType(String fileName) {
        String lowerName = lower(fileName);
        return lowerName.endsWith(".rdf") || lowerName.endsWith(".idm") ? "application/rdf+xml" : "application/xml";
    }

    private String baseName(String path) {
        int slash = path == null ? -1 : Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String sanitize(String fileName) {
        String value = fileName == null || fileName.isBlank() ? "model.rdf" : fileName;
        return value.replaceAll("[^A-Za-z0-9._/-]", "_");
    }

    private record RdfPayload(String relativePath, String fileName, byte[] bytes) {
    }

    private static final class ProcessingTimings {
        private final long startedAt = System.nanoTime();
        private final Map<String, Long> marks = new LinkedHashMap<>();
        private long previous = startedAt;

        private void mark(String name) {
            long now = System.nanoTime();
            marks.put(name, now - previous);
            previous = now;
        }

        private long elapsedMs(String name) {
            return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(marks.getOrDefault(name, 0L));
        }

        private long totalMs() {
            long total = 0L;
            for (Long value : marks.values()) {
                total += value;
            }
            return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(total);
        }
    }

    public ImportStatus importRdf(String fileName, byte[] payload, CnmServiceType serviceType, TimeFrame timeFrame) {
        String importId = UUID.randomUUID().toString();
        RdfPayload rdfPayload = new RdfPayload(fileName, baseName(fileName), payload);
        CnmImportFileDocument file = storePayload(importId, rdfPayload, timeFrame);
        CnmImportDocument document = new CnmImportDocument(
                importId,
                serviceType,
                timeFrame,
                file.state() == ImportFileState.FAILED ? ImportState.FAILED : ImportState.STARTED,
                List.of(file),
                Instant.now().toEpochMilli(),
                file.message());
        documentRepository.save(document);
        CnmImportDocument queuedDocument = publishTransformInitializationEvent(document);
        if (queuedDocument != document) {
            documentRepository.save(queuedDocument);
            document = queuedDocument;
        }
        logger.info("Imported CNM RDF file {} as {}", fileName, importId);
        return toStatus(document);
    }

    public ImportStatus processFile(CnmFileProcessingRequested event) {
        if (event == null) {
            throw new IllegalArgumentException("File-processing event is required");
        }
        CnmImportDocument current = findImportDocument(event.importId());
        CnmImportFileDocument target = current.files().stream()
                .filter(file -> file.fileId().equals(event.fileId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Import file not found: " + event.fileId()));
        return processFileUnlocked(event, current, target);
    }

    private ImportStatus processFileUnlocked(
            CnmFileProcessingRequested event,
            CnmImportDocument current,
            CnmImportFileDocument target) {
        ProcessingTimings timings = new ProcessingTimings();
        if (target.state() == ImportFileState.FAILED) {
            return toStatus(current);
        }
        if (target.state() == ImportFileState.PARSED) {
            return toStatus(current);
        }
        CnmProfileDocument existingProfile = existingProfile(current.id(), target.fileId());
        if (existingProfile != null) {
            CnmImportFileDocument processed = withParsedProfileDocument(target, existingProfile);
            ImportStatus status = completeProcessedFile(current, processed);
            timings.mark("metadataAlreadyExists");
            logMetadataProcessingTimings(current.id(), processed, null, timings);
            return status;
        }

        CnmImportFileDocument processed;
        RdfMetadata metadata = null;
        try {
            byte[] payload = objectStorageService.read(rawBucket, target.objectId());
            timings.mark("objectRead");
            ProfileProcessingContext context = processingContext(current.id(), target);
            metadata = metadataExtractor.extract(payload, context);
            timings.mark("rdfExtract");
            processed = withParsedMetadata(target, metadata);
            profileRepository.save(toProfileDocument(current.id(), processed, metadata));
            timings.mark("profileMetadataSave");
            saveProfilePayload(current.id(), processed, metadata);
            timings.mark("profilePayloadSave");
            saveProfileFragment(current.id(), processed, metadata);
            timings.mark("profileFragmentSave");
            if (mridIndexEnabled) {
                mridIndexRepository.saveAll(toMridIndexDocuments(current.id(), processed, metadata));
            }
            timings.mark("mridIndex");
        } catch (Exception exception) {
            logger.warn("Unable to process CNM RDF/XML metadata for {}", target.objectId(), exception);
            processed = withStatus(target, ImportFileState.FAILED, message(exception));
            timings.mark("failed");
        }

        ImportStatus status = completeProcessedFile(current, processed);
        timings.mark("statusUpdate");
        logMetadataProcessingTimings(current.id(), processed, metadata, timings);
        return status;
    }

    private CnmProfileDocument existingProfile(String importId, String fileId) {
        return profileRepository.findByField("id", fileId, 1)
                .stream()
                .filter(profile -> profile.importId().equals(importId))
                .findFirst()
                .orElse(null);
    }

    private ImportStatus completeProcessedFile(CnmImportDocument current, CnmImportFileDocument processed) {
        synchronized (importStatusLock(current.id())) {
            CnmImportDocument latest = findImportDocument(current.id());
            CnmImportFileDocument latestFile = latest.files().stream()
                    .filter(file -> file.fileId().equals(processed.fileId()))
                    .findFirst()
                    .orElse(processed);
            CnmImportFileDocument processedFile = mergeIidmStatus(processed, latestFile);
            List<CnmImportFileDocument> files = replaceFile(latest.files(), processedFile);
            CnmImportDocument updated = new CnmImportDocument(
                    latest.id(),
                    latest.serviceType(),
                    latest.timeFrame(),
                    aggregateState(files),
                    files,
                    latest.createdAt(),
                    aggregateMessage(latest.message(), files),
                    latest.iidmTransformationStatus());
            if (processed.state() != ImportFileState.FAILED) {
                publishSnapshotAssemblyIfComplete(updated, processed);
            }
            documentRepository.save(updated);
            if (processedFile.state() == ImportFileState.FAILED && !isSnapshotAssemblyFailure(processedFile)) {
                updateProfileStatus(processedFile.fileId(), ImportFileState.FAILED);
            }
            return toStatus(updated);
        }
    }

    private CnmImportFileDocument mergeIidmStatus(
            CnmImportFileDocument processed,
            CnmImportFileDocument latestFile) {
        return latestFile.iidmTransformationStatus() == IidmTransformationStatus.NOT_STARTED
                ? processed
                : withIidmTransformStatus(processed, latestFile.iidmTransformationStatus());
    }

    private void logMetadataProcessingTimings(
            String importId,
            CnmImportFileDocument file,
            RdfMetadata metadata,
            ProcessingTimings timings) {
        long totalMs = timings.totalMs();
        if (totalMs < slowMetadataProcessingThresholdMs && !logger.isDebugEnabled()) {
            return;
        }
        long factCount = metadata == null || metadata.fragment() == null ? 0 : metadata.fragment().facts().size();
        String message = "CNM RDF metadata timings import={} file={} state={} facts={} totalMs={} "
                + "objectReadMs={} rdfExtractMs={} profileMetadataSaveMs={} profilePayloadSaveMs={} "
                + "profileFragmentSaveMs={} mridIndexMs={} statusUpdateMs={} mridIndexEnabled={}";
        Object[] values = {
                importId,
                file.fileId(),
                file.state(),
                factCount,
                totalMs,
                timings.elapsedMs("objectRead"),
                timings.elapsedMs("rdfExtract"),
                timings.elapsedMs("profileMetadataSave"),
                timings.elapsedMs("profilePayloadSave"),
                timings.elapsedMs("profileFragmentSave"),
                timings.elapsedMs("mridIndex"),
                timings.elapsedMs("statusUpdate"),
                mridIndexEnabled
        };
        if (totalMs >= slowMetadataProcessingThresholdMs) {
            logger.warn(message, values);
        } else {
            logger.debug(message, values);
        }
    }

    private void logSnapshotAssemblyTimings(
            CnmSnapshotAssemblyRequested event,
            String snapshotId,
            String result,
            int fragmentCount,
            int payloadSectionCount,
            ProcessingTimings timings) {
        long totalMs = timings.totalMs();
        if (totalMs < slowMetadataProcessingThresholdMs && !logger.isDebugEnabled()) {
            return;
        }
        String message = "CNM snapshot assembly timings import={} snapshot={} group={} result={} "
                + "fragments={} payloadSections={} totalMs={} importReadMs={} groupNotReadyMs={} "
                + "fragmentLoadMs={} claimMs={} alreadyClaimedMs={} assembleMs={} payloadBuildMs={} "
                + "payloadSaveMs={} snapshotDoneSaveMs={} iidmPublishMs={} failedMs={}";
        Object[] values = {
                event.importId(),
                snapshotId,
                snapshotQueueKey(
                        event.importId(),
                        event.tsoName(),
                        event.businessDay(),
                        event.businessTime(),
                        event.modelTimeFrame()),
                result,
                fragmentCount,
                payloadSectionCount,
                totalMs,
                timings.elapsedMs("importRead"),
                timings.elapsedMs("groupNotReady"),
                timings.elapsedMs("fragmentLoad"),
                timings.elapsedMs("claim"),
                timings.elapsedMs("alreadyClaimed"),
                timings.elapsedMs("assemble"),
                timings.elapsedMs("payloadBuild"),
                timings.elapsedMs("payloadSave"),
                timings.elapsedMs("snapshotDoneSave"),
                timings.elapsedMs("iidmPublish"),
                timings.elapsedMs("failed")
        };
        if (totalMs >= slowMetadataProcessingThresholdMs) {
            logger.warn(message, values);
        } else {
            logger.debug(message, values);
        }
    }

    private boolean isSnapshotAssemblyFailure(CnmImportFileDocument file) {
        return file.message() != null && file.message().contains("Unable to assemble CGM network snapshot");
    }

    private List<CnmImportFileDocument> replaceFile(List<CnmImportFileDocument> files, CnmImportFileDocument replacement) {
        return files.stream()
                .map(file -> file.fileId().equals(replacement.fileId()) ? replacement : file)
                .toList();
    }

    private ProfileProcessingContext processingContext(String importId, CnmImportFileDocument file) {
        return ProfileProcessingContext.forFile(
                importId,
                file.fileId(),
                file.objectId(),
                file.tsoName(),
                file.businessDay(),
                file.businessTime(),
                file.modelTimeFrame(),
                file.profileFamily(),
                file.profileType());
    }

    private void publishSnapshotAssemblyIfComplete(CnmImportDocument document, CnmImportFileDocument processedFile) {
        List<CnmImportFileDocument> boundaryFiles = parsedBoundaryFiles(document);
        if (hasUnparsedBoundaryFiles(document)) {
            logger.info("Skipping IIDM transform publication for import {} until all boundary files are parsed", document.id());
            return;
        }
        if (isBoundaryProfile(processedFile)) {
            publishCompletedDirectTransforms(document, boundaryFiles);
            return;
        }
        List<CnmImportFileDocument> groupFiles = modelGroupFiles(document, processedFile);
        if (isIidmTransformReady(groupFiles)) {
            publishIidmTransformRequested(document.id(), withSupportFiles(groupFiles, boundaryFiles, matchingGlFiles(document, processedFile)));
            publishSnapshotAssemblyRequested(document, processedFile);
        } else {
            logger.info(
                    "Skipping IIDM transform publication for import {} group {} until required profiles are parsed; missing={}",
                    document.id(),
                    modelGroupKey(processedFile),
                    missingIidmProfileCodes(groupFiles));
        }
    }

    private void publishCompletedDirectTransforms(CnmImportDocument document, List<CnmImportFileDocument> boundaryFiles) {
        document.files().stream()
                .filter(file -> !isBoundaryProfile(file))
                .filter(file -> file.state() == ImportFileState.PARSED)
                .collect(java.util.stream.Collectors.groupingBy(
                        this::modelGroupKey,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()))
                .values()
                .stream()
                .filter(this::isIidmTransformReady)
                .forEach(groupFiles -> publishIidmTransformRequested(document.id(), withSupportFiles(
                        groupFiles,
                        boundaryFiles,
                        matchingGlFiles(document, groupFiles.getFirst()))));
    }

    private List<CnmImportFileDocument> modelGroupFiles(CnmImportDocument document, CnmImportFileDocument processedFile) {
        return document.files().stream()
                .filter(file -> !isBoundaryProfile(file))
                .filter(file -> sameModelGroup(file, processedFile))
                .toList();
    }

    private List<CnmImportFileDocument> parsedBoundaryFiles(CnmImportDocument document) {
        return document.files().stream()
                .filter(this::isBoundaryProfile)
                .filter(file -> file.state() == ImportFileState.PARSED)
                .toList();
    }

    private boolean hasUnparsedBoundaryFiles(CnmImportDocument document) {
        return document.files().stream()
                .filter(this::isBoundaryProfile)
                .anyMatch(file -> file.state() != ImportFileState.PARSED);
    }

    private List<CnmImportFileDocument> matchingGlFiles(CnmImportDocument document, CnmImportFileDocument modelFile) {
        return document.files().stream()
                .filter(this::isGlProfile)
                .filter(file -> file.state() == ImportFileState.PARSED)
                .filter(file -> sameValue(file.tsoName(), modelFile.tsoName()))
                .filter(file -> sameValue(file.businessDay(), modelFile.businessDay()))
                .toList();
    }

    private List<CnmImportFileDocument> withSupportFiles(
            List<CnmImportFileDocument> groupFiles,
            List<CnmImportFileDocument> boundaryFiles,
            List<CnmImportFileDocument> glFiles) {
        List<CnmImportFileDocument> files = new ArrayList<>(groupFiles);
        boundaryFiles.stream()
                .filter(boundary -> files.stream().noneMatch(file -> file.fileId().equals(boundary.fileId())))
                .forEach(files::add);
        glFiles.stream()
                .filter(glFile -> files.stream().noneMatch(file -> file.fileId().equals(glFile.fileId())))
                .forEach(files::add);
        return files;
    }

    private String modelGroupKey(CnmImportFileDocument file) {
        return valueOr(file.tsoName(), "")
                + "|"
                + valueOr(file.businessDay(), "")
                + "|"
                + valueOr(file.businessTime(), "")
                + "|"
                + valueOr(file.modelTimeFrame(), "");
    }

    private boolean isBoundaryProfile(CnmImportFileDocument file) {
        if (file == null) {
            return false;
        }
        CgmesProfileKind kind = CgmesProfileKind.fromCode(file.profileType());
        return kind == CgmesProfileKind.BOUNDARY_EQUIPMENT || kind == CgmesProfileKind.BOUNDARY_TOPOLOGY;
    }

    private boolean isGlProfile(CnmImportFileDocument file) {
        return file != null && CgmesProfileKind.fromCode(file.profileType()) == CgmesProfileKind.GEOGRAPHICAL_LOCATION;
    }

    private boolean isIidmTransformReady(List<CnmImportFileDocument> groupFiles) {
        if (groupFiles == null || groupFiles.isEmpty()) {
            return false;
        }
        return groupFiles.stream().allMatch(file -> file.state() == ImportFileState.PARSED)
                && profileKinds(groupFiles).containsAll(REQUIRED_IIDM_PROFILE_KINDS);
    }

    private Set<CgmesProfileKind> profileKinds(List<CnmImportFileDocument> groupFiles) {
        return groupFiles.stream()
                .map(file -> CgmesProfileKind.fromCode(file.profileType()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private List<String> missingIidmProfileCodes(List<CnmImportFileDocument> groupFiles) {
        Set<CgmesProfileKind> presentKinds = profileKinds(groupFiles == null ? List.of() : groupFiles);
        return REQUIRED_IIDM_PROFILE_KINDS.stream()
                .filter(kind -> !presentKinds.contains(kind))
                .map(CgmesProfileKind::code)
                .toList();
    }

    public void assembleSnapshot(CnmSnapshotAssemblyRequested event) {
        if (event == null) {
            throw new IllegalArgumentException("Snapshot assembly event is required");
        }
        ProcessingTimings timings = new ProcessingTimings();
        String snapshotId = snapshotId(
                event.importId(),
                event.tsoName(),
                event.businessDay(),
                event.businessTime(),
                event.modelTimeFrame());
        CnmImportDocument document = findImportDocument(event.importId());
        timings.mark("importRead");
        List<CnmImportFileDocument> groupFiles = document.files().stream()
                .filter(file -> sameModelGroup(file, event))
                .toList();
        if (groupFiles.isEmpty() || groupFiles.stream().anyMatch(file -> file.state() != ImportFileState.PARSED)) {
            timings.mark("groupNotReady");
            logger.info(
                    "Skipping CGM snapshot assembly for {} because the model group is not fully parsed",
                    snapshotQueueKey(event.importId(), event.tsoName(), event.businessDay(), event.businessTime(), event.modelTimeFrame()));
            logSnapshotAssemblyTimings(event, snapshotId, "GROUP_NOT_READY", 0, 0, timings);
            return;
        }
        List<ProfileFragment> fragments = groupFiles.stream()
                .map(file -> profileFragment(document.id(), file.fileId()))
                .flatMap(List::stream)
                .toList();
        timings.mark("fragmentLoad");
        if (fragments.isEmpty()) {
            logSnapshotAssemblyTimings(event, snapshotId, "NO_FRAGMENTS", 0, 0, timings);
            return;
        }
        if (!claimSnapshotAssembly(event, snapshotId)) {
            timings.mark("alreadyClaimed");
            logSnapshotAssemblyTimings(event, snapshotId, "ALREADY_CLAIMED", fragments.size(), 0, timings);
            return;
        }
        timings.mark("claim");
        CgmNetworkSnapshot snapshot = null;
        List<CnmNetworkSnapshotPayloadDocument> payloadSections = List.of();
        try {
            snapshot = snapshotAssembler.assemble(document.serviceType(), fragments);
            timings.mark("assemble");
            payloadSections = toNetworkSnapshotPayloadDocuments(snapshot);
            timings.mark("payloadBuild");
            networkSnapshotPayloadRepository.saveAll(payloadSections);
            timings.mark("payloadSave");
            networkSnapshotRepository.save(toNetworkSnapshotDocument(
                    snapshot,
                    payloadSections.size(),
                    CnmSnapshotState.DONE,
                    "CGM network snapshot assembly completed",
                    Instant.now().toEpochMilli()));
            timings.mark("snapshotDoneSave");
            publishIidmSnapshotTransformRequested(document.id(), snapshot);
            timings.mark("iidmPublish");
            logSnapshotAssemblyTimings(event, snapshotId, "DONE", fragments.size(), payloadSections.size(), timings);
        } catch (Exception exception) {
            timings.mark("failed");
            CgmNetworkSnapshot failedSnapshot = snapshot;
            networkSnapshotRepository.save(failedSnapshot == null
                    ? toStartedSnapshotDocument(event, CnmSnapshotState.FAILED, message(exception))
                    : toNetworkSnapshotDocument(
                            failedSnapshot,
                            payloadSections.size(),
                            CnmSnapshotState.FAILED,
                            message(exception),
                            Instant.now().toEpochMilli()));
            logSnapshotAssemblyTimings(event, snapshotId, "FAILED", fragments.size(), payloadSections.size(), timings);
            throw exception;
        }
    }

    private boolean claimSnapshotAssembly(CnmSnapshotAssemblyRequested event, String snapshotId) {
        String lockKey = snapshotQueueKey(
                event.importId(),
                event.tsoName(),
                event.businessDay(),
                event.businessTime(),
                event.modelTimeFrame());
        Object lock = snapshotLocks.computeIfAbsent(lockKey, ignored -> new Object());
        try {
            synchronized (lock) {
                if (snapshotDone(snapshotId, event.importId())) {
                    return false;
                }
                networkSnapshotRepository.save(toStartedSnapshotDocument(
                        event,
                        CnmSnapshotState.STARTED,
                        "CGM network snapshot assembly started"));
                return true;
            }
        } finally {
            snapshotLocks.remove(lockKey, lock);
        }
    }

    private boolean sameModelGroup(CnmImportFileDocument left, CnmImportFileDocument right) {
        return valueOr(left.tsoName(), "").equals(valueOr(right.tsoName(), ""))
                && valueOr(left.businessDay(), "").equals(valueOr(right.businessDay(), ""))
                && valueOr(left.businessTime(), "").equals(valueOr(right.businessTime(), ""))
                && valueOr(left.modelTimeFrame(), "").equals(valueOr(right.modelTimeFrame(), ""));
    }

    private boolean sameModelGroup(CnmImportFileDocument file, CnmSnapshotAssemblyRequested event) {
        return valueOr(file.tsoName(), "").equals(valueOr(event.tsoName(), ""))
                && valueOr(file.businessDay(), "").equals(valueOr(event.businessDay(), ""))
                && valueOr(file.businessTime(), "").equals(valueOr(event.businessTime(), ""))
                && valueOr(file.modelTimeFrame(), "").equals(valueOr(event.modelTimeFrame(), ""));
    }

    private void publishSnapshotAssemblyRequested(CnmImportDocument document, CnmImportFileDocument processedFile) {
        try {
            eventPublisher.publish(
                    eventExchange,
                    snapshotAssemblyRoutingKey,
                    new CnmSnapshotAssemblyRequested(
                            document.id(),
                            processedFile.tsoName(),
                            processedFile.businessDay(),
                            processedFile.businessTime(),
                            processedFile.modelTimeFrame(),
                            document.serviceType(),
                            document.timeFrame(),
                            0,
                            Instant.now()));
        } catch (Exception exception) {
            logger.warn(
                    "Unable to publish CGM snapshot assembly event for import {} file {}",
                    document.id(),
                    processedFile.fileId(),
                    exception);
        }
    }

    private boolean snapshotDone(CnmSnapshotAssemblyRequested event) {
        return snapshotDone(
                snapshotId(event.importId(), event.tsoName(), event.businessDay(), event.businessTime(), event.modelTimeFrame()),
                event.importId());
    }

    private boolean snapshotDone(String snapshotId, String importId) {
        return networkSnapshotRepository.findByField("id", snapshotId, 1)
                .stream()
                .filter(snapshot -> snapshot.importId().equals(importId))
                .anyMatch(snapshot -> snapshot.state() == CnmSnapshotState.STARTED || snapshot.state() == CnmSnapshotState.DONE);
    }

    private String snapshotQueueKey(
            String importId,
            String tsoName,
            String businessDay,
            String businessTime,
            String modelTimeFrame) {
        return String.join(":",
                valueOr(importId, ""),
                valueOr(tsoName, ""),
                valueOr(businessDay, ""),
                valueOr(businessTime, ""),
                valueOr(modelTimeFrame, ""));
    }

    private String snapshotId(
            String importId,
            String tsoName,
            String businessDay,
            String businessTime,
            String modelTimeFrame) {
        return snapshotQueueKey(importId, tsoName, businessDay, businessTime, modelTimeFrame);
    }

    private List<ProfileFragment> profileFragment(String importId, String fileId) {
        return profileFragmentRepository.findByField("id", fileId, 1)
                .stream()
                .filter(document -> document.importId().equals(importId))
                .map(this::profileFragment)
                .toList();
    }

    private ProfileFragment profileFragment(CnmProfileFragmentDocument document) {
        String json = document.fragmentJson();
        if ((json == null || json.isBlank()) && document.fragmentJsonChunks() != null) {
            json = String.join("", document.fragmentJsonChunks());
        }
        if ((json == null || json.isBlank()) && hasObjectPayload(document.payloadBucket(), document.payloadObjectKey())) {
            json = readObjectPayload(document.payloadBucket(), document.payloadObjectKey());
        }
        if (json == null || json.isBlank()) {
            json = joinedFragmentChunks(document.importId(), document.fileId());
        }
        return jsonMappingService.fromJson(json, ProfileFragment.class);
    }

    private void saveProfileFragment(String importId, CnmImportFileDocument file, RdfMetadata metadata) {
        ProfileFragment fragment = metadata.fragment();
        String fragmentJson = jsonMappingService.toJson(fragment);
        String objectKey = payloadObjectKey(importId, file.fileId(), "rdf-fragment.json");
        objectStorageService.store(profileFragmentBucket, objectKey, fragmentJson.getBytes(StandardCharsets.UTF_8), "application/json");
        profileFragmentRepository.save(toProfileFragmentDocument(importId, file, metadata, fragment, objectKey, fragmentJson));
    }

    private CnmProfileFragmentDocument toProfileFragmentDocument(
            String importId,
            CnmImportFileDocument file,
            RdfMetadata metadata,
            ProfileFragment fragment,
            String objectKey,
            String fragmentJson) {
        return new CnmProfileFragmentDocument(
                file.fileId(),
                importId,
                file.fileId(),
                file.objectId(),
                metadata.modelId(),
                metadata.family(),
                metadata.profileType(),
                file.tsoName(),
                file.businessDay(),
                file.businessTime(),
                file.modelTimeFrame(),
                file.modelVersion(),
                metadata.entityCounts(),
                fragment.facts().size(),
                metadata.warnings(),
                "",
                List.of(),
                0,
                profileFragmentBucket,
                objectKey,
                "application/json",
                sha256(fragmentJson),
                (long) fragmentJson.getBytes(StandardCharsets.UTF_8).length,
                file.uploadedAt());
    }

    private void saveFragmentChunks(String importId, CnmImportFileDocument file, List<String> chunks) {
        List<CnmProfileFragmentChunkDocument> documents = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            documents.add(new CnmProfileFragmentChunkDocument(
                    chunkDocumentId(file.fileId(), index),
                    importId,
                    file.fileId(),
                    index,
                    chunks.get(index),
                    file.uploadedAt()));
        }
        saveInSmallBatches(profileFragmentChunkRepository, documents);
    }

    private List<CnmMridIndexDocument> toMridIndexDocuments(
            String importId,
            CnmImportFileDocument file,
            RdfMetadata metadata) {
        long indexedAt = Instant.now().toEpochMilli();
        return metadata.fragment().facts().stream()
                .map(fact -> new CnmMridIndexDocument(
                        importId + ":" + file.fileId() + ":" + fact.mRID(),
                        importId,
                        fact.mRID(),
                        fact.cimType(),
                        fact.profileType(),
                        file.fileId(),
                        fact.references().values().stream().filter(value -> value != null && !value.isBlank()).toList(),
                        indexedAt))
                .toList();
    }

    private CnmNetworkSnapshotDocument toNetworkSnapshotDocument(
            CgmNetworkSnapshot snapshot,
            int payloadSectionCount,
            CnmSnapshotState state,
            String statusMessage,
            Object assembledAt) {
        return new CnmNetworkSnapshotDocument(
                snapshot.snapshotId(),
                snapshot.importId(),
                snapshot.serviceType(),
                snapshot.tsoName(),
                snapshot.businessDay(),
                snapshot.businessTime(),
                snapshot.timeFrame(),
                snapshot.sourceFileIds(),
                snapshot.staticTopology() == null ? 0 : snapshot.staticTopology().objects().size(),
                snapshot.staticTopology() == null ? 0 : snapshot.staticTopology().relations().size(),
                snapshot.stateSnapshot() == null ? 0 : snapshot.stateSnapshot().values().size(),
                snapshot.diagnostics().size(),
                payloadSectionCount,
                state,
                statusMessage,
                assembledAt == null ? Instant.now().toEpochMilli() : assembledAt);
    }

    private CnmNetworkSnapshotDocument toStartedSnapshotDocument(
            CnmSnapshotAssemblyRequested event,
            CnmSnapshotState state,
            String statusMessage) {
        return new CnmNetworkSnapshotDocument(
                snapshotId(
                        event.importId(),
                        event.tsoName(),
                        event.businessDay(),
                        event.businessTime(),
                        event.modelTimeFrame()),
                event.importId(),
                event.serviceType(),
                event.tsoName(),
                event.businessDay(),
                event.businessTime(),
                event.modelTimeFrame(),
                List.of(),
                0,
                0,
                0,
                0,
                0,
                state,
                statusMessage,
                Instant.now().toEpochMilli());
    }

    private List<CnmNetworkSnapshotPayloadDocument> toNetworkSnapshotPayloadDocuments(CgmNetworkSnapshot snapshot) {
        List<CnmNetworkSnapshotPayloadDocument> sections = new ArrayList<>();
        long createdAt = Instant.now().toEpochMilli();
        if (snapshot.staticTopology() != null) {
            sections.addAll(payloadDocuments(
                    snapshot,
                    "TOPOLOGY_OBJECTS",
                    snapshot.staticTopology().objects(),
                    createdAt));
            sections.addAll(payloadDocuments(
                    snapshot,
                    "TOPOLOGY_RELATIONS",
                    snapshot.staticTopology().relations(),
                    createdAt));
            sections.addAll(payloadDocuments(
                    snapshot,
                    "UNRESOLVED_REFERENCES",
                    snapshot.staticTopology().unresolvedReferences(),
                    createdAt));
        }
        if (snapshot.stateSnapshot() != null) {
            sections.addAll(payloadDocuments(snapshot, "STATE_VALUES", snapshot.stateSnapshot().values(), createdAt));
        }
        sections.addAll(payloadDocuments(snapshot, "DIAGNOSTICS", snapshot.diagnostics(), createdAt));
        return sections;
    }

    private List<CnmNetworkSnapshotPayloadDocument> payloadDocuments(
            CgmNetworkSnapshot snapshot,
            String section,
            List<?> values,
            long createdAt) {
        if (values == null || values.isEmpty()) {
            return List.of(payloadDocument(snapshot, section, 0, List.of(), createdAt));
        }
        List<CnmNetworkSnapshotPayloadDocument> documents = new ArrayList<>();
        List<String> batch = new ArrayList<>();
        int sequence = 0;
        int currentSize = 2;
        for (Object value : values) {
            String valueJson = jsonMappingService.toJson(value);
            int projectedSize = currentSize + valueJson.length() + (batch.isEmpty() ? 0 : 1);
            if (!batch.isEmpty() && projectedSize >= SNAPSHOT_PAYLOAD_SECTION_TARGET_SIZE) {
                documents.add(payloadDocument(snapshot, section, sequence++, batch, createdAt));
                batch = new ArrayList<>();
                currentSize = 2;
            }
            batch.add(valueJson);
            currentSize += valueJson.length() + (batch.size() == 1 ? 0 : 1);
        }
        if (!batch.isEmpty()) {
            documents.add(payloadDocument(snapshot, section, sequence, batch, createdAt));
        }
        return documents;
    }

    private CnmNetworkSnapshotPayloadDocument payloadDocument(
            CgmNetworkSnapshot snapshot,
            String section,
            int sequence,
            List<String> values,
            long createdAt) {
        String payloadJson = "[" + String.join(",", values) + "]";
        String objectKey = payloadObjectKey(
                snapshot.importId(),
                snapshot.snapshotId() + ":" + section + ":" + sequence,
                "snapshot-payload.json");
        objectStorageService.store(
                networkSnapshotPayloadBucket,
                objectKey,
                payloadJson.getBytes(StandardCharsets.UTF_8),
                "application/json");
        return new CnmNetworkSnapshotPayloadDocument(
                snapshot.snapshotId() + ":" + section + ":" + sequence,
                snapshot.snapshotId(),
                snapshot.importId(),
                section,
                sequence,
                values.size(),
                "",
                networkSnapshotPayloadBucket,
                objectKey,
                "application/json",
                sha256(payloadJson),
                (long) payloadJson.getBytes(StandardCharsets.UTF_8).length,
                createdAt);
    }

    private void publishIidmTransformRequested(String importId, List<CnmImportFileDocument> groupFiles) {
        if (groupFiles.isEmpty()) {
            return;
        }
        CnmImportFileDocument representative = groupFiles.getFirst();
        try {
            eventPublisher.publish(
                    iidmTransformExchange,
                    iidmTransformRoutingKey,
                    new IidmProfileTransformRequested(
                            importId,
                            directTransformFileId(groupFiles),
                            directTransformCorrelationKey(importId, groupFiles),
                            "",
                            "",
                            "CGMES_SOURCE",
                            representative.profileFamily(),
                            "",
                            representative.businessDay(),
                            representative.businessTime(),
                            representative.tsoName(),
                            representative.modelTimeFrame(),
                            groupFiles.stream()
                                    .map(file -> new CgmesIidmSourceFile(
                                            file.fileId(),
                                            file.fileName(),
                                            file.objectId(),
                                            file.profileFamily(),
                                            file.profileType()))
                                    .toList(),
                            new CgmesIidmImportOptions(Map.of())));
            logger.info(
                    "Published direct CGMES IIDM transform request for import {} with {} source files",
                    importId,
                    groupFiles.size());
        } catch (Exception exception) {
            logger.warn("Unable to publish direct CGMES IIDM transform event for {}", importId, exception);
        }
    }

    private String directTransformFileId(List<CnmImportFileDocument> groupFiles) {
        CnmImportFileDocument representative = groupFiles.getFirst();
        return representative.businessDay()
                + ":"
                + representative.businessTime()
                + ":"
                + representative.tsoName()
                + ":"
                + representative.modelTimeFrame();
    }

    private String directTransformCorrelationKey(String importId, List<CnmImportFileDocument> groupFiles) {
        return importId + ":" + directTransformFileId(groupFiles) + ":" + groupFiles.stream()
                .map(file -> file.fileId() + "=" + file.objectId())
                .sorted()
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private void publishIidmSnapshotTransformRequested(
            String importId,
            CgmNetworkSnapshot snapshot) {
        try {
            eventPublisher.publish(
                    iidmTransformExchange,
                    iidmTransformRoutingKey,
                    new IidmProfileTransformRequested(
                            importId,
                            snapshot.snapshotId(),
                            "",
                            snapshot.snapshotId(),
                            "SNAPSHOT",
                            ProfileFamily.CGMES,
                            snapshot.snapshotId(),
                            snapshot.businessDay(),
                            snapshot.businessTime(),
                            snapshot.tsoName(),
                            snapshot.timeFrame()));
        } catch (Exception exception) {
            logger.warn("Unable to publish IIDM snapshot transform event for {}", snapshot.snapshotId(), exception);
        }
    }

    public CnmPage<ImportStatus> listImports(int page, int size) {
        int boundedSize = size <= 0 ? 25 : size;
        int boundedPage = Math.max(page, 0);
        try {
            List<ImportStatus> imports = documentRepository.findAll(
                            Math.max((boundedPage + 1) * boundedSize, boundedSize),
                            DocumentSort.descending("createdAt"))
                    .stream()
                    .skip((long) boundedPage * boundedSize)
                    .limit(boundedSize)
                    .map(this::toStatus)
                    .toList();
            return new CnmPage<>(imports, imports.size(), boundedPage, boundedSize);
        } catch (RuntimeException exception) {
            logger.warn(
                    "Unable to list CNM imports using createdAt sort; retrying without sort. "
                            + "If this persists, recreate the cnm-imports Elasticsearch index.",
                    exception);
            DocumentPage<CnmImportDocument> fallback = documentRepository.search(new DocumentSearchRequest(
                    List.of(),
                    List.of(),
                    boundedPage,
                    boundedSize));
            List<ImportStatus> imports = fallback.content().stream()
                    .map(this::toStatus)
                    .toList();
            return new CnmPage<>(imports, fallback.total(), fallback.page(), fallback.size());
        }
    }

    public ImportStatus findImport(String importId) {
        return documentRepository.findByField("id", importId, 1)
                .stream()
                .findFirst()
                .map(this::toStatus)
                .orElseThrow(() -> new IllegalArgumentException("Import not found: " + importId));
    }

    public ImportStatus updateFileStatus(
            String importId,
            String fileId,
            ImportFileStatusUpdateRequest request) {
        synchronized (importStatusLock(importId)) {
            return updateFileStatusLocked(importId, fileId, request);
        }
    }

    private ImportStatus updateFileStatusLocked(
            String importId,
            String fileId,
            ImportFileStatusUpdateRequest request) {
        if (request == null || request.state() == null) {
            throw new IllegalArgumentException("File state is required");
        }
        CnmImportDocument current = findImportDocument(importId);
        boolean fileExists = current.files().stream().anyMatch(file -> file.fileId().equals(fileId));
        if (!fileExists) {
            throw new IllegalArgumentException("Import file not found: " + fileId);
        }
        List<CnmImportFileDocument> files = current.files().stream()
                .map(file -> file.fileId().equals(fileId)
                        ? withStatus(file, request.state(), request.message())
                        : file)
                .toList();
        ImportState aggregateState = aggregateState(files);
        CnmImportDocument updated = new CnmImportDocument(
                current.id(),
                current.serviceType(),
                current.timeFrame(),
                aggregateState,
                files,
                current.createdAt(),
                current.message(),
                current.iidmTransformationStatus());
        documentRepository.save(updated);
        updateProfileStatus(fileId, request.state());
        return toStatus(updated);
    }

    public void updateIidmTransformProgress(IidmProfileTransformStarted event) {
        if (event == null) {
            return;
        }
        updateIidmTransformProgress(
                event.importId(),
                event.fileId(),
                event.sourceFileIds(),
                IidmTransformationStatus.STARTED);
    }

    public void updateIidmTransformProgress(IidmProfileTransformCompleted event) {
        if (event == null) {
            return;
        }
        updateIidmTransformProgress(
                event.importId(),
                event.fileId(),
                event.sourceFileIds(),
                IidmTransformationStatus.DONE);
    }

    public void updateIidmTransformProgress(IidmProfileTransformFailed event) {
        if (event == null) {
            return;
        }
        updateIidmTransformProgress(
                event.importId(),
                event.fileId(),
                event.sourceFileIds(),
                IidmTransformationStatus.FAILED);
    }

    private void updateIidmTransformProgress(
            String importId,
            String fileId,
            List<String> sourceFileIds,
            IidmTransformationStatus eventStatus) {
        if (importId == null || importId.isBlank()) {
            return;
        }
        try {
            CnmImportDocument current = findImportDocument(importId);
            List<String> affectedFileIds = affectedFileIds(fileId, sourceFileIds);
            List<CnmImportFileDocument> files = current.files().stream()
                    .map(file -> affectedFileIds.contains(file.fileId())
                            ? withIidmTransformStatus(file, eventStatus)
                            : file)
                    .toList();
            CnmImportDocument updated = new CnmImportDocument(
                    current.id(),
                    current.serviceType(),
                    current.timeFrame(),
                    current.state(),
                    files,
                    current.createdAt(),
                    current.message(),
                    current.iidmTransformationStatus());
            IidmTransformationStatus nextStatus = aggregateIidmStatus(updated, eventStatus);
            documentRepository.save(new CnmImportDocument(
                    updated.id(),
                    updated.serviceType(),
                    updated.timeFrame(),
                    updated.state(),
                    files,
                    updated.createdAt(),
                    updated.message(),
                    nextStatus));
            logger.info("Updated IIDM transform status for import {} to {}", importId, nextStatus);
        } catch (Exception exception) {
            logger.warn("Unable to update IIDM transform status for import {}", importId, exception);
        }
    }

    private IidmTransformationStatus aggregateIidmStatus(
            CnmImportDocument document,
            IidmTransformationStatus eventStatus) {
        if (eventStatus == IidmTransformationStatus.FAILED
                || document.iidmTransformationStatus() == IidmTransformationStatus.FAILED) {
            return IidmTransformationStatus.FAILED;
        }
        List<IidmProfileTransformReadDocument> transforms = iidmTransforms(document.id());
        if (transforms.stream().anyMatch(transform -> transform.transformState() == eu.egm.data.iidm.common.IidmTransformState.FAILED)) {
            return IidmTransformationStatus.FAILED;
        }
        boolean importReadyForIidmCompletion = document.state() == ImportState.SUCCESS
                && document.files().stream()
                        .filter(file -> !isBoundaryProfile(file))
                        .allMatch(file -> file.state() == ImportFileState.PARSED);
        int expectedCount = expectedIidmTransformCount(document);
        boolean complete = importReadyForIidmCompletion
                && expectedCount > 0
                && transforms.size() >= expectedCount
                && transforms.stream().allMatch(transform -> transform.transformState() == eu.egm.data.iidm.common.IidmTransformState.DONE);
        if (complete) {
            return IidmTransformationStatus.DONE;
        }
        if (eventStatus == IidmTransformationStatus.STARTED
                || eventStatus == IidmTransformationStatus.DONE
                || !transforms.isEmpty()) {
            return IidmTransformationStatus.STARTED;
        }
        return document.iidmTransformationStatus() == IidmTransformationStatus.STARTED
                ? IidmTransformationStatus.STARTED
                : IidmTransformationStatus.NOT_STARTED;
    }

    private List<IidmProfileTransformReadDocument> iidmTransforms(String importId) {
        try {
            return iidmTransformRepository.findByField("importId", importId, 10_000);
        } catch (Exception exception) {
            logger.warn("Unable to read IIDM transform status for import {}", importId, exception);
            return List.of();
        }
    }

    private int expectedIidmTransformCount(CnmImportDocument document) {
        return (int) document.files().stream()
                .filter(file -> !isBoundaryProfile(file))
                .collect(java.util.stream.Collectors.groupingBy(
                        this::modelGroupKey,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()))
                .values()
                .stream()
                .filter(this::isIidmTransformReady)
                .count();
    }

    private Object importStatusLock(String importId) {
        String key = importId == null || importId.isBlank() ? "__unknown__" : importId;
        return importStatusLocks.computeIfAbsent(key, ignored -> new Object());
    }

    private List<String> affectedFileIds(String fileId, List<String> sourceFileIds) {
        List<String> ids = new ArrayList<>();
        if (sourceFileIds != null) {
            sourceFileIds.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(ids::add);
        }
        if (ids.isEmpty() && fileId != null && !fileId.isBlank()) {
            ids.add(fileId);
        }
        return ids;
    }

    private void updateProfileStatus(String fileId, ImportFileState state) {
        profileRepository.findByField("id", fileId, 1)
                .stream()
                .findFirst()
                .map(profile -> new CnmProfileDocument(
                        profile.id(),
                        profile.importId(),
                        profile.fileId(),
                        profile.fileName(),
                        profile.objectId(),
                        state,
                        profileFamily(profile),
                        profile.profileType(),
                        profile.detectedProfileKind(),
                        profile.modelId(),
                        profile.tsoName(),
                        profile.businessDay(),
                        profile.businessTime(),
                        profile.timeFrame(),
                        profile.version(),
                        profile.entityCounts(),
                        profile.warningCount(),
                        state == ImportFileState.FAILED ? Math.max(1, number(profile.errorCount())) : number(profile.errorCount()),
                        profile.profileJsonType(),
                        profile.importedAt()))
                .ifPresent(profileRepository::save);
    }

    private CnmImportDocument findImportDocument(String importId) {
        return documentRepository.findByField("id", importId, 1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Import not found: " + importId));
    }

    private CnmImportFileDocument withStatus(
            CnmImportFileDocument file,
            ImportFileState state,
            String statusMessage) {
        return new CnmImportFileDocument(
                file.fileId(),
                file.fileName(),
                file.objectId(),
                state,
                file.profileFamily(),
                file.businessDay(),
                file.businessTime(),
                file.modelTimeFrame(),
                file.tsoName(),
                file.profileType(),
                file.modelVersion(),
                file.profiles(),
                statusMessage == null || statusMessage.isBlank() ? file.message() : statusMessage.trim(),
                file.uploadedAt(),
                file.iidmTransformationStatus(),
                file.iidmTransformationCount(),
                file.iidmTransformationCompletedCount(),
                file.iidmTransformationFailedCount());
    }

    private CnmImportFileDocument withParsedProfileDocument(
            CnmImportFileDocument file,
            CnmProfileDocument profile) {
        return new CnmImportFileDocument(
                file.fileId(),
                file.fileName(),
                file.objectId(),
                ImportFileState.PARSED,
                profileFamily(profile),
                valueOr(profile.businessDay(), file.businessDay()),
                valueOr(profile.businessTime(), file.businessTime()),
                valueOr(profile.timeFrame(), file.modelTimeFrame()),
                valueOr(profile.tsoName(), file.tsoName()),
                valueOr(profile.profileType(), file.profileType()),
                valueOr(profile.version(), file.modelVersion()),
                file.profiles(),
                "RDF metadata already parsed",
                file.uploadedAt(),
                file.iidmTransformationStatus(),
                file.iidmTransformationCount(),
                file.iidmTransformationCompletedCount(),
                file.iidmTransformationFailedCount());
    }

    private CnmImportFileDocument withIidmTransformStatus(
            CnmImportFileDocument file,
            IidmTransformationStatus status) {
        return new CnmImportFileDocument(
                file.fileId(),
                file.fileName(),
                file.objectId(),
                file.state(),
                file.profileFamily(),
                file.businessDay(),
                file.businessTime(),
                file.modelTimeFrame(),
                file.tsoName(),
                file.profileType(),
                file.modelVersion(),
                file.profiles(),
                file.message(),
                file.uploadedAt(),
                status,
                file.iidmTransformationCount(),
                file.iidmTransformationCompletedCount(),
                file.iidmTransformationFailedCount());
    }

    private ImportState aggregateState(List<CnmImportFileDocument> files) {
        if (files.isEmpty()) {
            return ImportState.INIT;
        }
        if (files.stream().anyMatch(file -> file.state() == ImportFileState.FAILED)) {
            return ImportState.FAILED;
        }
        if (files.stream().anyMatch(file -> file.state() == ImportFileState.INIT)) {
            return ImportState.INIT;
        }
        if (files.stream().allMatch(file -> file.state() == ImportFileState.PARSED)) {
            return ImportState.SUCCESS;
        }
        return ImportState.IN_PROGRESS;
    }

    private String aggregateMessage(String currentMessage, List<CnmImportFileDocument> files) {
        ImportState state = aggregateState(files);
        return switch (state) {
            case SUCCESS -> "All CNM files processed successfully";
            case FAILED -> files.stream()
                    .filter(file -> file.state() == ImportFileState.FAILED)
                    .map(CnmImportFileDocument::message)
                    .filter(message -> message != null && !message.isBlank())
                    .findFirst()
                    .orElse("One or more CNM files failed processing");
            default -> currentMessage;
        };
    }

    private CnmImportFileDocument withParsedMetadata(CnmImportFileDocument file, RdfMetadata metadata) {
        return new CnmImportFileDocument(
                file.fileId(),
                file.fileName(),
                file.objectId(),
                ImportFileState.PARSED,
                file.profileFamily(),
                file.businessDay(),
                file.businessTime(),
                file.modelTimeFrame(),
                file.tsoName(),
                file.profileType(),
                file.modelVersion(),
                metadata.profiles(),
                "RDF metadata parsed",
                file.uploadedAt(),
                file.iidmTransformationStatus(),
                file.iidmTransformationCount(),
                file.iidmTransformationCompletedCount(),
                file.iidmTransformationFailedCount());
    }

    public CnmPage<CnmProfileMetadata> searchProfiles(
            String importId,
            String profileType,
            String tsoName,
            String businessDay,
            String businessTime,
            int page,
            int size) {
        List<DocumentFilter> filters = new ArrayList<>();
        if (importId != null && !importId.isBlank()) {
            filters.add(DocumentFilter.exact("importId", importId.trim()));
        }
        if (profileType != null && !profileType.isBlank()) {
            filters.add(DocumentFilter.exact("profileType", profileType.trim().toUpperCase(Locale.ROOT)));
        }
        if (tsoName != null && !tsoName.isBlank()) {
            filters.add(DocumentFilter.exact("tsoName", tsoName));
        }
        if (businessDay != null && !businessDay.isBlank()) {
            filters.add(DocumentFilter.exact("businessDay", businessDay));
        }
        if (businessTime != null && !businessTime.isBlank()) {
            filters.add(DocumentFilter.exact("businessTime", businessTime));
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(size <= 0 ? 25 : size, MAX_PROFILE_PAGE_SIZE);
        DocumentPage<CnmProfileDocument> result =
                profileRepository.search(new DocumentSearchRequest(
                        filters,
                        List.of(),
                        List.of(),
                        PROFILE_LIST_EXCLUDED_FIELDS,
                        safePage,
                        safeSize));
        return new CnmPage<>(
                result.content().stream().map(this::toProfileMetadata).toList(),
                result.total(),
                result.page(),
                result.size());
    }

    public CnmPage<CnmSnapshotMetadata> searchSnapshots(String importId, CnmSnapshotState state, int page, int size) {
        List<DocumentFilter> filters = new ArrayList<>();
        if (importId != null && !importId.isBlank()) {
            filters.add(DocumentFilter.exact("importId", importId.trim()));
        }
        if (state != null) {
            filters.add(DocumentFilter.exact("state", state.name()));
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(size <= 0 ? 25 : size, MAX_PROFILE_PAGE_SIZE);
        DocumentPage<CnmNetworkSnapshotDocument> result =
                networkSnapshotRepository.search(new DocumentSearchRequest(
                        filters,
                        List.of(),
                        List.of(),
                        List.of(),
                        safePage,
                        safeSize));
        return new CnmPage<>(
                result.content().stream().map(this::toSnapshotMetadata).toList(),
                result.total(),
                result.page(),
                result.size());
    }

    public Object profilePayload(String importId, String fileId) {
        CnmProfileDocument document = findProfileDocument(importId, fileId);
        String profileJson = profileJson(importId, fileId);
        if (profileJson == null || profileJson.isBlank()) {
            throw new IllegalArgumentException("Profile payload is not available for file: " + fileId);
        }
        return jsonMappingService.fromJson(profileJson, Map.class);
    }

    public DynamicTableBundle profileTables(String importId, String fileId) {
        CnmProfileDocument document = findProfileDocument(importId, fileId);
        String profileJson = profileJson(importId, fileId);
        if (profileJson == null || profileJson.isBlank()) {
            throw new IllegalArgumentException("Profile table data is not available for file: " + fileId);
        }
        Map<String, Object> payload = jsonMappingService.fromJson(profileJson, Map.class);
        return toDynamicTableBundle(importId, fileId, document, payload);
    }

    public DynamicTableDefinition profileTable(String importId, String fileId, String tableId) {
        return profileTables(importId, fileId).tables().stream()
                .filter(table -> table.tableId().equals(tableId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile table not found: " + tableId));
    }

    private CnmProfileDocument findProfileDocument(String importId, String fileId) {
        return profileRepository.findByField("id", fileId, 1)
                .stream()
                .filter(document -> document.importId().equals(importId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile data not found for file: " + fileId));
    }

    private CnmProfileDocument toProfileDocument(String importId, CnmImportFileDocument file, RdfMetadata metadata) {
        return new CnmProfileDocument(
                file.fileId(),
                importId,
                file.fileId(),
                file.fileName(),
                file.objectId(),
                file.state(),
                file.profileFamily(),
                valueOr(metadata.profileType(), file.profileType()),
                metadata.detectedProfileKind(),
                metadata.modelId(),
                file.tsoName(),
                file.businessDay(),
                file.businessTime(),
                file.modelTimeFrame(),
                file.modelVersion(),
                toEntityCounts(metadata.entityCounts()),
                metadata.warnings().size(),
                0,
                metadata.profileJsonType(),
                file.uploadedAt());
    }

    private void saveProfilePayload(String importId, CnmImportFileDocument file, RdfMetadata metadata) {
        String profileJson = jsonMappingService.toJson(metadata.payload());
        String objectKey = payloadObjectKey(importId, file.fileId(), "profile-payload.json");
        objectStorageService.store(profilePayloadBucket, objectKey, profileJson.getBytes(StandardCharsets.UTF_8), "application/json");
        profilePayloadRepository.save(toProfilePayloadDocument(importId, file, metadata, objectKey, profileJson));
    }

    private CnmProfilePayloadDocument toProfilePayloadDocument(
            String importId,
            CnmImportFileDocument file,
            RdfMetadata metadata,
            String objectKey,
            String profileJson) {
        return new CnmProfilePayloadDocument(
                file.fileId(),
                importId,
                file.fileId(),
                metadata.profileJsonType(),
                "",
                List.of(),
                0,
                profilePayloadBucket,
                objectKey,
                "application/json",
                sha256(profileJson),
                (long) profileJson.getBytes(StandardCharsets.UTF_8).length,
                file.uploadedAt());
    }

    private void savePayloadChunks(
            String importId,
            CnmImportFileDocument file,
            RdfMetadata metadata,
            List<String> chunks) {
        List<CnmProfilePayloadChunkDocument> documents = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            documents.add(new CnmProfilePayloadChunkDocument(
                    chunkDocumentId(file.fileId(), index),
                    importId,
                    file.fileId(),
                    index,
                    metadata.profileJsonType(),
                    chunks.get(index),
                    file.uploadedAt()));
        }
        saveInSmallBatches(profilePayloadChunkRepository, documents);
    }

    private <T> void saveInSmallBatches(DocumentRepositoryService<T> repository, List<T> documents) {
        for (int index = 0; index < documents.size(); index += PROFILE_JSON_CHUNK_BATCH_SIZE) {
            repository.saveAll(documents.subList(index, Math.min(index + PROFILE_JSON_CHUNK_BATCH_SIZE, documents.size())));
        }
    }

    private String profileJson(String importId, String fileId) {
        CnmProfilePayloadDocument payload = profilePayloadRepository.findByField("id", fileId, 1)
                .stream()
                .filter(candidate -> candidate.importId().equals(importId))
                .findFirst()
                .orElse(null);
        if (payload == null) {
            return "";
        }
        if (payload.profileJson() != null && !payload.profileJson().isBlank()) {
            return payload.profileJson();
        }
        if (hasObjectPayload(payload.payloadBucket(), payload.payloadObjectKey())) {
            return readObjectPayload(payload.payloadBucket(), payload.payloadObjectKey());
        }
        if (payload.profileJsonChunks() == null || payload.profileJsonChunks().isEmpty()) {
            return joinedPayloadChunks(importId, fileId);
        }
        return String.join("", payload.profileJsonChunks());
    }

    private String joinedPayloadChunks(String importId, String fileId) {
        return profilePayloadChunkRepository.findByField("fileId", fileId, chunkFetchLimit())
                .stream()
                .filter(chunk -> chunk.importId().equals(importId))
                .sorted(Comparator.comparing(CnmProfilePayloadChunkDocument::chunkIndex))
                .map(CnmProfilePayloadChunkDocument::chunkJson)
                .reduce("", String::concat);
    }

    private String joinedFragmentChunks(String importId, String fileId) {
        return profileFragmentChunkRepository.findByField("fileId", fileId, chunkFetchLimit())
                .stream()
                .filter(chunk -> chunk.importId().equals(importId))
                .sorted(Comparator.comparing(CnmProfileFragmentChunkDocument::chunkIndex))
                .map(CnmProfileFragmentChunkDocument::chunkJson)
                .reduce("", String::concat);
    }

    private boolean hasObjectPayload(String bucket, String objectKey) {
        return bucket != null && !bucket.isBlank() && objectKey != null && !objectKey.isBlank();
    }

    private String readObjectPayload(String bucket, String objectKey) {
        return new String(objectStorageService.read(bucket, objectKey), StandardCharsets.UTF_8);
    }

    private String payloadObjectKey(String importId, String fileId, String suffix) {
        return safeObjectName(importId) + "/" + safeObjectName(fileId) + "/" + suffix;
    }

    private String safeObjectName(String value) {
        return value == null ? "payload" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }

    private int chunkFetchLimit() {
        return 10_000;
    }

    private String chunkDocumentId(String fileId, int chunkIndex) {
        return fileId + ":" + chunkIndex;
    }

    private List<String> chunks(String value) {
        if (value == null || value.length() <= PROFILE_JSON_CHUNK_SIZE) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (int index = 0; index < value.length(); index += PROFILE_JSON_CHUNK_SIZE) {
            chunks.add(value.substring(index, Math.min(index + PROFILE_JSON_CHUNK_SIZE, value.length())));
        }
        return chunks;
    }

    private List<CnmEntityCountDocument> toEntityCounts(Map<String, Long> counts) {
        if (counts == null || counts.isEmpty()) {
            return List.of();
        }
        return counts.entrySet().stream()
                .map(entry -> new CnmEntityCountDocument(entry.getKey(), entry.getValue() == null ? 0 : entry.getValue()))
                .toList();
    }

    private DynamicTableBundle toDynamicTableBundle(
            String importId,
            String fileId,
            CnmProfileDocument document,
            Map<String, Object> payload) {
        List<DynamicTableDefinition> tables = new ArrayList<>();
        tables.add(table("topologyObjects", "Topology objects", listOfMaps(payload.get("topologyObjects"))));
        tables.add(table("topologyRelations", "Topology relations", listOfMaps(payload.get("topologyRelations"))));
        Object profile = payload.get("profile");
        if (profile instanceof Map<?, ?> profileMap) {
            profileMap.forEach((key, value) -> {
                if (value instanceof List<?> list) {
                    tables.add(table(String.valueOf(key), label(String.valueOf(key)), listOfMaps(list)));
                } else if (value != null) {
                    tables.add(table(String.valueOf(key), label(String.valueOf(key)), List.of(toMap(value))));
                }
            });
        } else if (profile != null) {
            tables.add(table("profile", "Profile", List.of(toMap(profile))));
        }
        return new DynamicTableBundle(
                importId,
                fileId,
                document.profileType(),
                profileFamily(document),
                payload,
                tables.stream().filter(table -> !table.rows().isEmpty()).toList());
    }

    private DynamicTableDefinition table(String tableId, String label, List<Map<String, Object>> sourceRows) {
        List<String> keys = sourceRows.stream()
                .flatMap(row -> row.keySet().stream())
                .distinct()
                .toList();
        List<DynamicTableColumn> columns = keys.stream()
                .map(key -> new DynamicTableColumn(key, label(key), type(sourceRows, key), true, true, ""))
                .toList();
        List<DynamicTableRow> rows = new ArrayList<>();
        for (int index = 0; index < sourceRows.size(); index++) {
            Map<String, Object> row = sourceRows.get(index);
            String rowId = String.valueOf(row.getOrDefault("mRID", row.getOrDefault("rowId", tableId + "-" + index)));
            rows.add(new DynamicTableRow(rowId, row));
        }
        String defaultSort = columns.isEmpty() ? "" : columns.get(0).key();
        return new DynamicTableDefinition(tableId, label, columns, rows, rows.size(), defaultSort);
    }

    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(this::toMap).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, entryValue) -> {
                if (entryValue == null || entryValue instanceof String || entryValue instanceof Number || entryValue instanceof Boolean) {
                    result.put(String.valueOf(key), entryValue);
                } else if (entryValue instanceof List<?> list) {
                    result.put(String.valueOf(key), list.size());
                } else {
                    result.put(String.valueOf(key), String.valueOf(entryValue));
                }
            });
            return result;
        }
        return jsonMappingService.fromJson(jsonMappingService.toJson(value), Map.class);
    }

    private String type(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .map(row -> row.get(key))
                .filter(value -> value != null)
                .findFirst()
                .map(value -> value instanceof Number ? "number" : value instanceof Boolean ? "boolean" : "string")
                .orElse("string");
    }

    private String label(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    private CnmProfileMetadata toProfileMetadata(CnmProfileDocument document) {
        CnmModelFileName fileNameMetadata = parseModelFileName(document.fileName());
        return new CnmProfileMetadata(
                document.id(),
                document.fileId(),
                document.importId(),
                document.fileName(),
                document.objectId(),
                document.state() == null ? ImportFileState.PARSED : document.state(),
                profileFamily(document),
                valueOr(document.profileType(), fileNameMetadata.profileType()),
                valueOr(document.tsoName(), fileNameMetadata.tsoName()),
                valueOr(document.businessDay(), fileNameMetadata.businessDay()),
                valueOr(document.businessTime(), fileNameMetadata.businessTime()),
                valueOr(document.timeFrame(), fileNameMetadata.timeFrame()),
                valueOr(document.version(), fileNameMetadata.version()),
                instant(document.importedAt()));
    }

    private CnmSnapshotMetadata toSnapshotMetadata(CnmNetworkSnapshotDocument document) {
        return new CnmSnapshotMetadata(
                document.id(),
                document.importId(),
                document.serviceType(),
                document.tsoName(),
                document.businessDay(),
                document.businessTime(),
                document.timeFrame(),
                document.sourceFileIds(),
                number(document.staticObjectCount()),
                number(document.relationCount()),
                number(document.stateValueCount()),
                number(document.diagnosticCount()),
                number(document.payloadSectionCount()),
                document.state(),
                document.message(),
                instant(document.assembledAt()));
    }

    private ProfileFamily profileFamily(CnmProfileDocument document) {
        return document.profileFamily() == null
                ? ProfileFamily.fromCode(document.profileType())
                : document.profileFamily();
    }

    private ImportStatus toStatus(CnmImportDocument document) {
        List<ImportFileStatus> files = document.files().stream()
                .map(this::toFileStatus)
                .toList();
        return new ImportStatus(
                document.id(),
                document.serviceType(),
                document.timeFrame(),
                document.state(),
                files,
                instant(document.createdAt()),
                document.message(),
                effectiveIidmStatus(document));
    }

    private IidmTransformationStatus effectiveIidmStatus(CnmImportDocument document) {
        return aggregateIidmStatus(document, IidmTransformationStatus.NOT_STARTED);
    }

    private ImportFileStatus toFileStatus(CnmImportFileDocument file) {
        CnmModelFileName fileNameMetadata = parseModelFileName(file.fileName());
        ProfileFamily family = file.profileFamily() == null || file.profileFamily() == ProfileFamily.Unknown
                ? fileNameMetadata.profileFamily()
                : file.profileFamily();
        return new ImportFileStatus(
                file.fileId(),
                file.fileName(),
                file.objectId(),
                file.state() == null ? ImportFileState.INIT : file.state(),
                family,
                valueOr(file.businessDay(), fileNameMetadata.businessDay()),
                valueOr(file.businessTime(), fileNameMetadata.businessTime()),
                valueOr(file.modelTimeFrame(), fileNameMetadata.timeFrame()),
                valueOr(file.tsoName(), fileNameMetadata.tsoName()),
                valueOr(file.profileType(), fileNameMetadata.profileType()),
                valueOr(file.modelVersion(), fileNameMetadata.version()),
                file.profiles(),
                file.message(),
                file.iidmTransformationStatus(),
                number(file.iidmTransformationCount()),
                number(file.iidmTransformationCompletedCount()),
                number(file.iidmTransformationFailedCount()),
                instant(file.uploadedAt()));
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean sameValue(String left, String right) {
        return valueOr(left, "").equalsIgnoreCase(valueOr(right, ""));
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        String text = value.toString().trim();
        if (text.matches("-?\\d+")) {
            return Instant.ofEpochMilli(Long.parseLong(text));
        }
        return Instant.parse(text);
    }
}
