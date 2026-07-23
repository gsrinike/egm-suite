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
import eu.egm.data.cnm.common.CnmFileProcessingRequested;
import eu.egm.data.cnm.common.CnmPage;
import eu.egm.data.cnm.common.CnmProfileMetadata;
import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.DynamicTableBundle;
import eu.egm.data.cnm.common.DynamicTableColumn;
import eu.egm.data.cnm.common.DynamicTableDefinition;
import eu.egm.data.cnm.common.DynamicTableRow;
import eu.egm.data.cnm.common.ImportFailureRequest;
import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ImportFileStatus;
import eu.egm.data.cnm.common.ImportFileStatusUpdateRequest;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.ImportStatus;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.TimeFrame;
import eu.egm.data.iidm.common.IidmProfileTransformRequested;
import eu.egm.mapping.JacksonJsonMappingService;
import eu.egm.mapping.JsonMappingService;
import eu.egm.srv.cnm.services.domain.CnmImportDocument;
import eu.egm.srv.cnm.services.domain.CnmImportDocument.CnmImportFileDocument;
import eu.egm.srv.cnm.services.domain.CnmImportDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmProfileDocument;
import eu.egm.srv.cnm.services.domain.CnmProfileDocument.CnmEntityCountDocument;
import eu.egm.srv.cnm.services.domain.CnmProfileDocumentAdapter;
import eu.egm.srv.cnm.services.domain.CnmProfilePayloadDocument;
import eu.egm.srv.cnm.services.domain.CnmProfilePayloadDocumentAdapter;
import eu.egm.srv.cnm.services.rdf.ProfileProcessingContext;
import eu.egm.srv.cnm.services.rdf.RdfMetadata;
import eu.egm.srv.cnm.services.rdf.RdfMetadataExtractor;
import io.micrometer.observation.ObservationRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CnmImportRestService extends RestServiceSupport {
    private static final Pattern MODEL_FILE_PATTERN =
            Pattern.compile(
                    "^(?<timestamp>\\d{8}T\\d{4}Z)_(?<timeFrame>ID|1D|2D)_(?<tso>.+?)_(?<profile>[A-Z0-9_-]+)_(?<version>\\d+)$",
                    Pattern.CASE_INSENSITIVE);
    private static final int PROFILE_JSON_CHUNK_SIZE = 1_000_000;
    private static final int MAX_PROFILE_PAGE_SIZE = 50;
    private static final List<String> PROFILE_LIST_EXCLUDED_FIELDS = List.of("profileJson", "profileJsonChunks");

    private final ObjectStorageService objectStorageService;
    private final DocumentRepositoryService<CnmImportDocument> documentRepository;
    private final DocumentRepositoryService<CnmProfileDocument> profileRepository;
    private final DocumentRepositoryService<CnmProfilePayloadDocument> profilePayloadRepository;
    private final EventPublisherService eventPublisher;
    private final JsonMappingService jsonMappingService;
    private final RdfMetadataExtractor metadataExtractor;
    private final String rawBucket;
    private final String eventExchange;
    private final String fileProcessingRoutingKey;
    private final String iidmTransformExchange;
    private final String iidmTransformRoutingKey;
    private final ConcurrentMap<String, Object> processingLocks = new ConcurrentHashMap<>();

    public CnmImportRestService(
            Environment environment,
            ObservationRegistry observationRegistry,
            InfrastructureUtils infrastructureUtils,
            RdfMetadataExtractor metadataExtractor,
            @Value("${cnm.import.raw-bucket:cnm-rdf-models}") String rawBucket,
            @Value("${cnm.import.event.exchange:cnm.events}") String eventExchange,
            @Value("${cnm.import.event.file-processing-routing-key:cnm.file.processing.requested}") String fileProcessingRoutingKey,
            @Value("${cnm.import.event.iidm-transform-exchange:iidm.events}") String iidmTransformExchange,
            @Value("${cnm.import.event.iidm-transform-routing-key:iidm.profile.transform.requested}") String iidmTransformRoutingKey) {
        this(
                environment,
                observationRegistry,
                infrastructureUtils,
                new JacksonJsonMappingService(),
                metadataExtractor,
                rawBucket,
                eventExchange,
                fileProcessingRoutingKey,
                iidmTransformExchange,
                iidmTransformRoutingKey);
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
            @Value("${cnm.import.event.iidm-transform-exchange:iidm.events}") String iidmTransformExchange,
            @Value("${cnm.import.event.iidm-transform-routing-key:iidm.profile.transform.requested}") String iidmTransformRoutingKey) {
        super(environment, observationRegistry);
        this.objectStorageService = infrastructureUtils.objectStorageService();
        this.documentRepository = infrastructureUtils.documentRepository(new CnmImportDocumentAdapter());
        this.profileRepository = infrastructureUtils.documentRepository(new CnmProfileDocumentAdapter());
        this.profilePayloadRepository = infrastructureUtils.documentRepository(new CnmProfilePayloadDocumentAdapter());
        this.eventPublisher = infrastructureUtils.eventPublisher();
        this.jsonMappingService = jsonMappingService;
        this.metadataExtractor = metadataExtractor;
        this.rawBucket = rawBucket;
        this.eventExchange = eventExchange;
        this.fileProcessingRoutingKey = fileProcessingRoutingKey;
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
                    .map(payload -> CompletableFuture.supplyAsync(() -> storePayload(importId, payload), executorService))
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparing(CnmImportFileDocument::fileName))
                    .toList();
            ImportState state = files.stream().anyMatch(file -> file.state() == ImportFileState.FAILED)
                    ? ImportState.FAILED
                    : ImportState.STORED;
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
            CnmImportDocument queuedDocument = publishProcessingEvents(document);
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
                .map(fileName -> statusFile(fileName, state, message, createdAt))
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
            String message,
            Instant createdAt) {
        String safeFileName = fileName == null || fileName.isBlank() ? "upload" : fileName;
        ModelFileName modelFileName = parseModelFileName(safeFileName);
        return new CnmImportFileDocument(
                UUID.randomUUID().toString(),
                safeFileName,
                "",
                toFileState(state),
                ProfileFamily.Unknown,
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
            case STORED -> ImportFileState.STORED;
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

    private CnmImportFileDocument storePayload(String importId, RdfPayload payload) {
        Instant uploadedAt = Instant.now();
        ModelFileName modelFileName = parseModelFileName(payload.fileName());
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

    private CnmImportDocument publishProcessingEvents(CnmImportDocument document) {
        List<CnmImportFileDocument> files = document.files();
        List<CnmImportFileDocument> updatedFiles = new ArrayList<>(files.size());
        boolean changed = false;
        for (CnmImportFileDocument file : files) {
            if (file.state() != ImportFileState.STORED) {
                updatedFiles.add(file);
                continue;
            }
            try {
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
                                0,
                                Instant.now()));
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
                aggregateState(updatedFiles),
                updatedFiles,
                document.createdAt(),
                "One or more files could not be queued for metadata processing");
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

    private ModelFileName parseModelFileName(String fileName) {
        String baseName = baseName(fileName);
        int dot = baseName.lastIndexOf('.');
        String stem = dot > 0 ? baseName.substring(0, dot) : baseName;
        Matcher matcher = MODEL_FILE_PATTERN.matcher(stem);
        if (!matcher.matches()) {
            return ModelFileName.empty();
        }
        String timestamp = matcher.group("timestamp");
        return new ModelFileName(
                LocalDate.parse(timestamp.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE).toString(),
                LocalTime.parse(timestamp.substring(9, 13), DateTimeFormatter.ofPattern("HHmm")).toString(),
                matcher.group("timeFrame").toUpperCase(Locale.ROOT),
                matcher.group("tso"),
                matcher.group("profile").toUpperCase(Locale.ROOT).replace('-', '_'),
                matcher.group("version"),
                ProfileFamily.fromCode(matcher.group("profile")));
    }

    private String contentType(String fileName) {
        return lower(fileName).endsWith(".rdf") ? "application/rdf+xml" : "application/xml";
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

    private record ModelFileName(
            String businessDay,
            String businessTime,
            String timeFrame,
            String tsoName,
            String profileType,
            String version,
            ProfileFamily profileFamily) {
        static ModelFileName empty() {
            return new ModelFileName("", "", "", "", "", "", ProfileFamily.Unknown);
        }
    }

    public ImportStatus importRdf(String fileName, byte[] payload, CnmServiceType serviceType, TimeFrame timeFrame) {
        String importId = UUID.randomUUID().toString();
        RdfPayload rdfPayload = new RdfPayload(fileName, baseName(fileName), payload);
        CnmImportFileDocument file = storePayload(importId, rdfPayload);
        CnmImportDocument document = new CnmImportDocument(
                importId,
                serviceType,
                timeFrame,
                aggregateState(List.of(file)),
                List.of(file),
                Instant.now().toEpochMilli(),
                file.message());
        documentRepository.save(document);
        CnmImportDocument queuedDocument = publishProcessingEvents(document);
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
        ProfileProcessingContext context = processingContext(current.id(), target);
        Object lock = processingLocks.computeIfAbsent(context.queueKey(), ignored -> new Object());
        synchronized (lock) {
            return processFileLocked(event);
        }
    }

    private ImportStatus processFileLocked(CnmFileProcessingRequested event) {
        CnmImportDocument current = findImportDocument(event.importId());
        CnmImportFileDocument target = current.files().stream()
                .filter(file -> file.fileId().equals(event.fileId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Import file not found: " + event.fileId()));
        if (target.state() == ImportFileState.PARSED || target.state() == ImportFileState.FAILED) {
            return toStatus(current);
        }

        CnmImportFileDocument processed;
        try {
            byte[] payload = objectStorageService.read(rawBucket, target.objectId());
            ProfileProcessingContext context = processingContext(current.id(), target);
            RdfMetadata metadata = metadataExtractor.extract(payload, context);
            processed = withParsedMetadata(target, metadata);
            profileRepository.save(toProfileDocument(current.id(), processed, metadata));
            profilePayloadRepository.save(toProfilePayloadDocument(current.id(), processed, metadata));
            publishIidmTransformRequested(current.id(), processed, metadata);
        } catch (Exception exception) {
            logger.warn("Unable to process CNM RDF/XML metadata for {}", target.objectId(), exception);
            processed = withStatus(target, ImportFileState.FAILED, message(exception));
        }

        CnmImportFileDocument processedFile = processed;
        List<CnmImportFileDocument> files = current.files().stream()
                .map(file -> file.fileId().equals(target.fileId()) ? processedFile : file)
                .toList();
        CnmImportDocument updated = new CnmImportDocument(
                current.id(),
                current.serviceType(),
                current.timeFrame(),
                aggregateState(files),
                files,
                current.createdAt(),
                aggregateMessage(current.message(), files));
        documentRepository.save(updated);
        if (processed.state() == ImportFileState.FAILED) {
            updateProfileStatus(processed.fileId(), ImportFileState.FAILED);
        }
        return toStatus(updated);
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

    private void publishIidmTransformRequested(String importId, CnmImportFileDocument file, RdfMetadata metadata) {
        try {
            eventPublisher.publish(
                    iidmTransformExchange,
                    iidmTransformRoutingKey,
                    new IidmProfileTransformRequested(
                            importId,
                            file.fileId(),
                            file.fileId(),
                            valueOr(metadata.profileType(), file.profileType()),
                            file.profileFamily(),
                            file.objectId(),
                            file.businessDay(),
                            file.businessTime(),
                            file.tsoName(),
                            file.modelTimeFrame()));
        } catch (Exception exception) {
            logger.warn("Unable to publish IIDM transform event for {}", file.fileId(), exception);
        }
    }

    public CnmPage<ImportStatus> listImports(int page, int size) {
        int boundedSize = size <= 0 ? 25 : size;
        List<ImportStatus> imports = documentRepository.findAll(
                        Math.max((page + 1) * boundedSize, boundedSize),
                        DocumentSort.descending("createdAt"))
                .stream()
                .skip((long) Math.max(page, 0) * boundedSize)
                .limit(boundedSize)
                .map(this::toStatus)
                .toList();
        return new CnmPage<>(imports, imports.size(), Math.max(page, 0), boundedSize);
    }

    public ImportStatus findImport(String importId) {
        return documentRepository.findByField("id", importId, 1)
                .stream()
                .findFirst()
                .map(this::toStatus)
                .orElseThrow(() -> new IllegalArgumentException("Import not found: " + importId));
    }

    public synchronized ImportStatus updateFileStatus(
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
                current.message());
        documentRepository.save(updated);
        updateProfileStatus(fileId, request.state());
        return toStatus(updated);
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
                file.uploadedAt());
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
        return ImportState.STORED;
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
                file.uploadedAt());
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

    private CnmProfilePayloadDocument toProfilePayloadDocument(
            String importId,
            CnmImportFileDocument file,
            RdfMetadata metadata) {
        String profileJson = jsonMappingService.toJson(metadata.payload());
        return new CnmProfilePayloadDocument(
                file.fileId(),
                importId,
                file.fileId(),
                metadata.profileJsonType(),
                profileJson.length() <= PROFILE_JSON_CHUNK_SIZE ? profileJson : "",
                chunks(profileJson),
                file.uploadedAt());
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
        if (payload.profileJsonChunks() == null || payload.profileJsonChunks().isEmpty()) {
            return "";
        }
        return String.join("", payload.profileJsonChunks());
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
        ModelFileName fileNameMetadata = parseModelFileName(document.fileName());
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
                document.message());
    }

    private ImportFileStatus toFileStatus(CnmImportFileDocument file) {
        ModelFileName fileNameMetadata = parseModelFileName(file.fileName());
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
                instant(file.uploadedAt()));
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
