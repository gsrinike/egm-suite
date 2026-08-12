package eu.egm.srv.iidm.transformer.service;

import com.infra.InfrastructureUtils;
import com.infra.event.EventPublisherService;
import com.infra.storage.document.DocumentFilter;
import com.infra.storage.document.DocumentPage;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSearchRequest;
import com.infra.storage.object.ObjectStorageService;
import com.utils.restservice.RestServiceSupport;
import com.utils.profile.ProfileDefaults;
import com.utils.profile.ProfileDefaultsService;
import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import eu.egm.data.cnm.common.DynamicTableColumn;
import eu.egm.data.cnm.common.DynamicTableDefinition;
import eu.egm.data.cnm.common.DynamicTableRow;
import eu.egm.data.cnm.common.CnmSnapshotState;
import eu.egm.data.cnm.common.GridTopologyObject;
import eu.egm.data.cnm.common.GridTopologyRelation;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.cnm.snapshot.CgmNetworkSnapshot;
import eu.egm.data.cnm.state.StateSnapshot;
import eu.egm.data.cnm.state.StateVariablePoint;
import eu.egm.data.cnm.topology.StaticTopologyModel;
import eu.egm.data.iidm.common.IidmProfileTransformCompleted;
import eu.egm.data.iidm.common.IidmProfileTransformFailed;
import eu.egm.data.iidm.common.IidmNetworkMergeFailed;
import eu.egm.data.iidm.common.IidmNetworkMergeState;
import eu.egm.data.iidm.common.IidmNetworkMergeStatus;
import eu.egm.data.iidm.common.IidmProfileTransformRequested;
import eu.egm.data.iidm.common.IidmProfileTransformStarted;
import eu.egm.data.iidm.common.IidmDiagnostic;
import eu.egm.data.iidm.common.IidmTransformState;
import eu.egm.data.iidm.common.CgmesIidmSourceFile;
import eu.egm.data.iidm.network.IidmNetworkModel;
import eu.egm.data.iidm.network.IidmNetworkSummary;
import eu.egm.data.iidm.network.IidmNetworkXiidm;
import eu.egm.map.cnm.iidm.CgmesSourceToIidmMappingConfiguration;
import eu.egm.map.cnm.iidm.CgmesSourceToIidmTransformer;
import eu.egm.map.cnm.iidm.CnmToIidmMappingConfiguration;
import eu.egm.map.cnm.iidm.CnmToIidmTransformer;
import eu.egm.mapping.JsonMappingService;
import eu.egm.mapping.ReflectionMappingService;
import eu.egm.srv.iidm.transformer.domain.CnmProfileReadDocument;
import eu.egm.srv.iidm.transformer.domain.CnmProfileReadDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.CnmProfilePayloadChunkReadDocument;
import eu.egm.srv.iidm.transformer.domain.CnmProfilePayloadChunkReadDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.CnmProfilePayloadReadDocument;
import eu.egm.srv.iidm.transformer.domain.CnmProfilePayloadReadDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.CnmNetworkSnapshotReadDocument;
import eu.egm.srv.iidm.transformer.domain.CnmNetworkSnapshotReadDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.CnmNetworkSnapshotPayloadReadDocument;
import eu.egm.srv.iidm.transformer.domain.CnmNetworkSnapshotPayloadReadDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.IidmNetworkDocument;
import eu.egm.srv.iidm.transformer.domain.IidmNetworkDocument.IidmElementCountDocument;
import eu.egm.srv.iidm.transformer.domain.IidmNetworkDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.IidmGridViewDocument;
import eu.egm.srv.iidm.transformer.domain.IidmGridViewDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.IidmProfileTransformDocument;
import eu.egm.srv.iidm.transformer.domain.IidmProfileTransformDocumentAdapter;
import eu.egm.srv.iidm.transformer.api.IidmElementCountResponse;
import eu.egm.srv.iidm.transformer.api.IidmGridViewMapDataResponse;
import eu.egm.srv.iidm.transformer.api.IidmGridViewMapDataResponse.GridViewBounds;
import eu.egm.srv.iidm.transformer.api.IidmGridViewMapDataResponse.GridViewLine;
import eu.egm.srv.iidm.transformer.api.IidmGridViewMapDataResponse.GridViewPoint;
import eu.egm.srv.iidm.transformer.api.IidmGridViewMapResponse;
import eu.egm.srv.iidm.transformer.api.IidmNetworkSummaryResponse;
import eu.egm.srv.iidm.transformer.api.IidmPage;
import eu.egm.srv.iidm.transformer.api.IidmTableBundle;
import eu.egm.srv.iidm.transformer.api.IidmTransformSummaryResponse;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class IidmProfileTransformService extends RestServiceSupport {
    private static final int NETWORK_XIIDM_CHUNK_SIZE = 1_000_000;

    private final DocumentRepositoryService<CnmProfileReadDocument> sourceProfileRepository;
    private final DocumentRepositoryService<CnmProfilePayloadReadDocument> sourcePayloadRepository;
    private final DocumentRepositoryService<CnmProfilePayloadChunkReadDocument> sourcePayloadChunkRepository;
    private final DocumentRepositoryService<CnmNetworkSnapshotReadDocument> sourceSnapshotRepository;
    private final DocumentRepositoryService<CnmNetworkSnapshotPayloadReadDocument> sourceSnapshotPayloadRepository;
    private final DocumentRepositoryService<IidmProfileTransformDocument> transformRepository;
    private final DocumentRepositoryService<IidmNetworkDocument> networkRepository;
    private final DocumentRepositoryService<IidmGridViewDocument> gridViewRepository;
    private final ObjectStorageService objectStorageService;
    private final EventPublisherService eventPublisher;
    private final JsonMappingService jsonMappingService;
    private final CnmToIidmTransformer transformer;
    private final CgmesSourceToIidmTransformer sourceTransformer;
    private final IidmNetworkJsonProjection networkJsonProjection = new IidmNetworkJsonProjection();
    private final String rawBucket;
    private final String networkPayloadBucket;
    private final String gridViewBucket;
    private final String eventExchange;
    private final String startedRoutingKey;
    private final String completedRoutingKey;
    private final String failedRoutingKey;
    private final String mergeStartedRoutingKey;
    private final String mergeCompletedRoutingKey;
    private final String mergeFailedRoutingKey;

    public IidmProfileTransformService(
            Environment environment,
            ObservationRegistry observationRegistry,
            InfrastructureUtils infrastructureUtils,
            JsonMappingService jsonMappingService,
            @Value("${iidm.transform.raw-bucket:cnm-rdf-models}") String rawBucket,
            @Value("${iidm.transform.network-payload-bucket:iidm-network-payloads}") String networkPayloadBucket,
            @Value("${iidm.grid-view.bucket:iidm-grid-view}") String gridViewBucket,
            @Value("${iidm.transform.event.exchange:iidm.events}") String eventExchange,
            @Value("${iidm.transform.event.started-routing-key:iidm.profile.transform.started}") String startedRoutingKey,
            @Value("${iidm.transform.event.completed-routing-key:iidm.profile.transform.completed}") String completedRoutingKey,
            @Value("${iidm.transform.event.failed-routing-key:iidm.profile.transform.failed}") String failedRoutingKey,
            @Value("${iidm.transform.event.merge-started-routing-key:iidm.network.merge.started}") String mergeStartedRoutingKey,
            @Value("${iidm.transform.event.merge-completed-routing-key:iidm.network.merge.completed}") String mergeCompletedRoutingKey,
            @Value("${iidm.transform.event.merge-failed-routing-key:iidm.network.merge.failed}") String mergeFailedRoutingKey) {
        super(environment, observationRegistry);
        this.sourceProfileRepository = infrastructureUtils.documentRepository(new CnmProfileReadDocumentAdapter());
        this.sourcePayloadRepository = infrastructureUtils.documentRepository(new CnmProfilePayloadReadDocumentAdapter());
        this.sourcePayloadChunkRepository =
                infrastructureUtils.documentRepository(new CnmProfilePayloadChunkReadDocumentAdapter());
        this.sourceSnapshotRepository = infrastructureUtils.documentRepository(new CnmNetworkSnapshotReadDocumentAdapter());
        this.sourceSnapshotPayloadRepository = infrastructureUtils.documentRepository(new CnmNetworkSnapshotPayloadReadDocumentAdapter());
        this.transformRepository = infrastructureUtils.documentRepository(new IidmProfileTransformDocumentAdapter());
        this.networkRepository = infrastructureUtils.documentRepository(new IidmNetworkDocumentAdapter());
        this.gridViewRepository = infrastructureUtils.documentRepository(new IidmGridViewDocumentAdapter());
        this.objectStorageService = infrastructureUtils.objectStorageService();
        this.eventPublisher = infrastructureUtils.eventPublisher();
        this.jsonMappingService = jsonMappingService;
        this.transformer = new CnmToIidmTransformer(new ReflectionMappingService(), iidmMappingConfiguration());
        this.sourceTransformer = new CgmesSourceToIidmTransformer(new ReflectionMappingService(), cgmesSourceMappingConfiguration());
        this.rawBucket = rawBucket;
        this.networkPayloadBucket = networkPayloadBucket;
        this.gridViewBucket = gridViewBucket;
        this.eventExchange = eventExchange;
        this.startedRoutingKey = startedRoutingKey;
        this.completedRoutingKey = completedRoutingKey;
        this.failedRoutingKey = failedRoutingKey;
        this.mergeStartedRoutingKey = mergeStartedRoutingKey;
        this.mergeCompletedRoutingKey = mergeCompletedRoutingKey;
        this.mergeFailedRoutingKey = mergeFailedRoutingKey;
    }

    private CnmToIidmMappingConfiguration iidmMappingConfiguration() {
        ProfileDefaults defaults = new ProfileDefaultsService().load("iidm", "defaults");
        return new CnmToIidmMappingConfiguration(
                defaults.doubleValue("iidm.defaults.nominal-voltage", 400.0),
                defaults.stringValue("iidm.defaults.substation-id", "EGM_DEFAULT_SUBSTATION"),
                defaults.stringValue("iidm.defaults.substation-name", "Default Substation"),
                defaults.stringValue("iidm.defaults.voltage-level-id", "EGM_DEFAULT_VL"),
                defaults.stringValue("iidm.defaults.voltage-level-name", "Default Voltage Level"),
                defaults.stringValue("iidm.defaults.bus-id", "EGM_DEFAULT_BUS"),
                defaults.stringValue("iidm.defaults.bus-name", "Default Bus"),
                defaults.doubleValue("iidm.defaults.line-x", 0.0001));
    }

    private CgmesSourceToIidmMappingConfiguration cgmesSourceMappingConfiguration() {
        ProfileDefaults defaults = new ProfileDefaultsService().load("iidm", "defaults");
        Map<String, String> properties = new LinkedHashMap<>();
        defaults.values().forEach((key, value) -> {
            if (key.startsWith("iidm.import.cgmes.")) {
                properties.put(key, String.valueOf(value));
            }
        });
        return new CgmesSourceToIidmMappingConfiguration(properties);
    }

    public void transform(IidmProfileTransformRequested request) {
        if (request == null) {
            throw new IllegalArgumentException("IIDM transform request is required");
        }
        long startedAt = Instant.now().toEpochMilli();
        String transformId = transformId(request);
        String networkId = networkId(request.importId(), request.fileId());
        List<String> sourceFileIds = sourceFileIds(request);
        List<String> sourceFileNames = sourceFileNames(request);
        transformRepository.save(new IidmProfileTransformDocument(
                transformId,
                request.importId(),
                request.fileId(),
                request.transformCorrelationKey(),
                request.objectId(),
                sourceFileIds,
                sourceFileNames,
                request.profileType(),
                request.profileFamily(),
                request.sourceProfilePayloadId(),
                IidmTransformState.STARTED,
                "IIDM transformation started",
                List.of(),
                networkId,
                startedAt,
                null,
                null));
        eventPublisher.publish(eventExchange, startedRoutingKey,
                new IidmProfileTransformStarted(
                        request.importId(),
                        request.fileId(),
                        transformId,
                        sourceFileIds,
                        sourceFileNames,
                        networkId,
                        "STARTED"));
        IidmTransformDiagnosticsCollector diagnosticsCollector = new IidmTransformDiagnosticsCollector(networkId);
        try {
            IidmNetworkModel network = transformNetwork(request);
            networkRepository.save(toNetworkDocument(network));
            long completedAt = Instant.now().toEpochMilli();
            List<IidmDiagnostic> diagnostics = diagnosticsCollector.successDiagnostics(network.diagnostics());
            transformRepository.save(new IidmProfileTransformDocument(
                    transformId,
                    request.importId(),
                    request.fileId(),
                    request.transformCorrelationKey(),
                    request.objectId(),
                    sourceFileIds,
                    sourceFileNames,
                    request.profileType(),
                    request.profileFamily(),
                    request.sourceProfilePayloadId(),
                    IidmTransformState.DONE,
                    "IIDM transformation completed",
                    diagnostics,
                    network.id(),
                    startedAt,
                    completedAt,
                    null));
            eventPublisher.publish(eventExchange, completedRoutingKey,
                    new IidmProfileTransformCompleted(
                            request.importId(),
                            request.fileId(),
                            transformId,
                            sourceFileIds,
                            sourceFileNames,
                            network.id(),
                            "DONE"));
        } catch (Exception exception) {
            long failedAt = Instant.now().toEpochMilli();
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            List<IidmDiagnostic> diagnostics = diagnosticsCollector.failureDiagnostics(exception, networkId);
            logger.warn(
                    "IIDM transformation failed importId={} fileId={} transformId={} networkId={} sourceFiles={} message={}",
                    request.importId(),
                    request.fileId(),
                    transformId,
                    networkId,
                    sourceFileNames,
                    message,
                    exception);
            transformRepository.save(new IidmProfileTransformDocument(
                    transformId,
                    request.importId(),
                    request.fileId(),
                    request.transformCorrelationKey(),
                    request.objectId(),
                    sourceFileIds,
                    sourceFileNames,
                    request.profileType(),
                    request.profileFamily(),
                    request.sourceProfilePayloadId(),
                    IidmTransformState.FAILED,
                    message,
                    diagnostics,
                    networkId,
                    startedAt,
                    null,
                    failedAt));
            eventPublisher.publish(eventExchange, failedRoutingKey,
                    new IidmProfileTransformFailed(request.importId(), request.fileId(), transformId, sourceFileIds, sourceFileNames, message));
        } finally {
            diagnosticsCollector.close();
        }
    }

    public void merge(IidmNetworkMergeStatus request) {
        if (request == null || request.importId() == null || request.importId().isBlank()) {
            throw new IllegalArgumentException("IIDM merge request importId is required");
        }
        String importId = request.importId();
        String mergedNetworkId = mergedNetworkId(importId);
        eventPublisher.publish(
                eventExchange,
                mergeStartedRoutingKey,
                new IidmNetworkMergeStatus(
                        importId,
                        mergedNetworkId,
                        request.iidmNetworkIds(),
                        request.sourceFiles(),
                        IidmNetworkMergeState.STARTED,
                        request.businessDay(),
                        request.businessTime(),
                        request.timeFrame(),
                        request.tsoName(),
                        request.importOptions(),
                        Instant.now().toString(),
                        "Merged IIDM network creation started"));
        try {
            if (mergedNetworkExists(mergedNetworkId)) {
                eventPublisher.publish(
                        eventExchange,
                        mergeCompletedRoutingKey,
                        new IidmNetworkMergeStatus(
                                importId,
                                mergedNetworkId,
                                request.iidmNetworkIds(),
                                request.sourceFiles(),
                                IidmNetworkMergeState.COMPLETED,
                                request.businessDay(),
                                request.businessTime(),
                                request.timeFrame(),
                                request.tsoName(),
                                request.importOptions(),
                                Instant.now().toString(),
                                "Merged IIDM network already exists"));
                return;
            }
            IidmNetworkModel mergedNetwork = request.sourceFiles().isEmpty()
                    ? mergeExistingNetworks(request, mergedNetworkId)
                    : transformMergedCgmesSource(request, mergedNetworkId);
            networkRepository.save(toNetworkDocument(mergedNetwork));
            eventPublisher.publish(
                    eventExchange,
                    mergeCompletedRoutingKey,
                    new IidmNetworkMergeStatus(
                            importId,
                            mergedNetworkId,
                            request.iidmNetworkIds(),
                            request.sourceFiles(),
                            IidmNetworkMergeState.COMPLETED,
                            request.businessDay(),
                            request.businessTime(),
                            request.timeFrame(),
                            request.tsoName(),
                            request.importOptions(),
                            Instant.now().toString(),
                            "Merged IIDM network persisted"));
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            logger.warn("Merged IIDM network creation failed importId={} networkId={}", importId, mergedNetworkId, exception);
            eventPublisher.publish(
                    eventExchange,
                    mergeFailedRoutingKey,
                    new IidmNetworkMergeFailed(importId, mergedNetworkId, Instant.now().toString(), message));
        }
    }

    private IidmNetworkModel transformMergedCgmesSource(IidmNetworkMergeStatus request, String mergedNetworkId) {
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("egm-cgmes-iidm-merge-");
            for (int index = 0; index < request.sourceFiles().size(); index++) {
                CgmesIidmSourceFile source = request.sourceFiles().get(index);
                byte[] bytes = objectStorageService.read(rawBucket, source.objectId());
                Path target = workspace.resolve(index + "-" + safeFileName(source));
                Files.write(target, bytes);
            }
            logger.info(
                    "Creating merged IIDM network {} from {} CGMES source file(s)",
                    mergedNetworkId,
                    request.sourceFiles().size());
            return sourceTransformer.transform(
                    workspace,
                    mergedNetworkId,
                    request.importId(),
                    request.sourceFiles().stream().map(this::sourceFileReference).toList(),
                    request.businessDay(),
                    request.businessTime(),
                    request.timeFrame(),
                    request.tsoName(),
                    request.importOptions().properties());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to stage CGMES source files for merged IIDM transformation", exception);
        } finally {
            deleteWorkspace(workspace);
        }
    }

    private IidmNetworkModel mergeExistingNetworks(IidmNetworkMergeStatus request, String mergedNetworkId) {
        List<IidmNetworkDocument> documents = sourceNetworkDocuments(request);
        if (documents.isEmpty()) {
            throw new IllegalStateException("No completed IIDM network documents found for import " + request.importId());
        }
        List<Network> networks = documents.stream()
                .map(document -> IidmNetworkXiidm.read(networkXiidm(document)))
                .toList();
        Network merged = mergeNetworks(networks);
        List<String> sourceFileIds = documents.stream()
                .flatMap(document -> document.sourceFileIds().stream())
                .distinct()
                .sorted()
                .toList();
        IidmNetworkDocument first = documents.getFirst();
        List<IidmDiagnostic> diagnostics = List.of(new IidmDiagnostic(
                "INFO",
                "IIDM_MERGE",
                "Merged " + documents.size() + " IIDM network document(s)",
                mergedNetworkId));
        return new IidmNetworkModel(
                mergedNetworkId,
                request.importId(),
                sourceFileIds,
                first.businessDay(),
                first.businessTime(),
                first.timeFrame(),
                "MERGED_CGM",
                merged,
                diagnostics);
    }

    private IidmNetworkModel transformNetwork(IidmProfileTransformRequested request) {
        if (isDirectCgmesSourceRequest(request)) {
            return transformCgmesSource(request);
        }
        return request.sourceSnapshotId() == null || request.sourceSnapshotId().isBlank()
                ? transformProfilePayload(request)
                : transformSnapshot(request);
    }

    private boolean mergedNetworkExists(String mergedNetworkId) {
        return networkRepository.findByField("id", mergedNetworkId, 1).stream().findFirst().isPresent();
    }

    private List<IidmNetworkDocument> sourceNetworkDocuments(IidmNetworkMergeStatus request) {
        List<IidmNetworkDocument> documents = request.iidmNetworkIds().isEmpty()
                ? networkRepository.findByField("importId", request.importId(), 10_000)
                : request.iidmNetworkIds().stream()
                        .flatMap(networkId -> networkRepository.findByField("id", networkId, 1).stream())
                        .toList();
        return documents.stream()
                .filter(document -> !mergedNetworkId(request.importId()).equals(document.id()))
                .filter(document -> document.networkXiidm() != null && !document.networkXiidm().isBlank()
                        || hasObjectPayload(document.networkXiidmBucket(), document.networkXiidmObjectKey())
                        || !document.networkXiidmChunks().isEmpty())
                .sorted(Comparator.comparing(IidmNetworkDocument::id))
                .toList();
    }

    private Network mergeNetworks(List<Network> networks) {
        if (networks.isEmpty()) {
            throw new IllegalArgumentException("At least one IIDM network is required for merge");
        }
        if (networks.size() == 1) {
            return networks.getFirst();
        }
        try {
            Class<?> mergerClass = Class.forName("com.powsybl.iidm.network.NetworkMerger");
            Object merger = mergerClass.getDeclaredConstructor().newInstance();
            Method merge = mergerClass.getMethod("merge", Network.class, Network.class);
            Network merged = networks.getFirst();
            for (int index = 1; index < networks.size(); index++) {
                Object result = merge.invoke(merger, merged, networks.get(index));
                if (result instanceof Network network) {
                    merged = network;
                }
            }
            return merged;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("PowSyBl NetworkMerger is required to merge multiple IIDM networks", exception);
        }
    }

    private boolean isDirectCgmesSourceRequest(IidmProfileTransformRequested request) {
        return (request.sourceProfilePayloadId() == null || request.sourceProfilePayloadId().isBlank())
                && (request.sourceSnapshotId() == null || request.sourceSnapshotId().isBlank())
                && request.sourceFiles() != null
                && request.sourceFiles().stream().anyMatch(file -> file.objectId() != null && !file.objectId().isBlank());
    }

    private IidmNetworkModel transformCgmesSource(IidmProfileTransformRequested request) {
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("egm-cgmes-iidm-");
            for (int index = 0; index < request.sourceFiles().size(); index++) {
                CgmesIidmSourceFile source = request.sourceFiles().get(index);
                byte[] bytes = objectStorageService.read(rawBucket, source.objectId());
                Path target = workspace.resolve(index + "-" + safeFileName(source));
                Files.write(target, bytes);
            }
            return sourceTransformer.transform(
                    workspace,
                    networkId(request.importId(), request.fileId()),
                    request.importId(),
                    request.sourceFiles().stream().map(this::sourceFileReference).toList(),
                    request.businessDay(),
                    request.businessTime(),
                    request.timeFrame(),
                    request.tsoName(),
                    request.importOptions().properties());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to stage CGMES source files for IIDM transformation", exception);
        } finally {
            deleteWorkspace(workspace);
        }
    }

    private String safeFileName(CgmesIidmSourceFile source) {
        String fileName = source.fileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = source.objectId();
            int slash = fileName.lastIndexOf('/');
            fileName = slash >= 0 && slash < fileName.length() - 1 ? fileName.substring(slash + 1) : fileName;
        }
        String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "cgmes-profile.xml" : sanitized;
    }

    private void deleteWorkspace(Path workspace) {
        if (workspace == null) {
            return;
        }
        try (var stream = Files.walk(workspace)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Temporary staging cleanup is best effort.
                }
            });
        } catch (IOException ignored) {
            // Temporary staging cleanup is best effort.
        }
    }

    private IidmNetworkModel transformProfilePayload(IidmProfileTransformRequested request) {
        CnmProfilePayloadReadDocument sourcePayload = sourcePayload(request.sourceProfilePayloadId(), request.importId());
        ProfilePayload<?> profilePayload = jsonMappingService.fromJson(profileJson(sourcePayload), ProfilePayload.class);
        return transformer.transform(
                profilePayload,
                request.importId(),
                request.businessDay(),
                request.businessTime(),
                request.timeFrame(),
                request.tsoName());
    }

    private IidmNetworkModel transformSnapshot(IidmProfileTransformRequested request) {
        CnmNetworkSnapshotReadDocument sourceSnapshot = sourceSnapshot(request.sourceSnapshotId(), request.importId());
        if (sourceSnapshot.state() != CnmSnapshotState.DONE) {
            throw new IllegalStateException("CNM network snapshot is not ready: " + sourceSnapshot.id());
        }
        CgmNetworkSnapshot snapshot = reconstructSnapshot(sourceSnapshot);
        return transformer.transform(snapshot);
    }

    public List<IidmProfileTransformDocument> transforms(String importId, int maxResults) {
        if (importId == null || importId.isBlank()) {
            return transformRepository.findAll(maxResults, new com.infra.storage.document.DocumentSort(
                    "startedAt",
                    com.infra.storage.document.DocumentSort.Direction.DESC));
        }
        return transformRepository.findByField("importId", importId, maxResults);
    }

    public IidmPage<IidmTransformSummaryResponse> transformSummaries(String importId, String search, int page, int size) {
        DocumentPage<IidmProfileTransformDocument> documents = transformRepository.search(new DocumentSearchRequest(
                filters(importId),
                iidmTransformSearchFilters(search),
                page,
                size));
        return new IidmPage<>(
                documents.content().stream().map(this::toTransformSummary).toList(),
                documents.total(),
                documents.page(),
                documents.size());
    }

    public IidmProfileTransformDocument transformByFileId(String fileId) {
        return transformRepository.findByField("id", fileId, 1)
                .stream()
                .findFirst()
                .or(() -> transformRepository.findByField("fileId", fileId, 1).stream().findFirst())
                .or(() -> transformRepository.findByField("transformCorrelationKey", fileId, 1).stream().findFirst())
                .or(() -> transformRepository.findByField("sourceFileIds", fileId, 10).stream().findFirst())
                .orElseThrow(() -> new IllegalArgumentException("IIDM transform not found: " + fileId));
    }

    public List<IidmNetworkDocument> networks(String importId, int maxResults) {
        if (importId == null || importId.isBlank()) {
            return networkRepository.findAll(maxResults, new com.infra.storage.document.DocumentSort(
                    "updatedAt",
                    com.infra.storage.document.DocumentSort.Direction.DESC));
        }
        return networkRepository.findByField("importId", importId, maxResults);
    }

    public IidmPage<IidmNetworkSummaryResponse> networkSummaries(String importId, int page, int size) {
        DocumentPage<IidmNetworkDocument> documents = networkRepository.search(new DocumentSearchRequest(
                filters(importId),
                List.of(),
                List.of(),
                List.of("networkXiidm", "networkXiidmChunks", "networkJson", "networkJsonChunks"),
                page,
                size));
        return new IidmPage<>(
                documents.content().stream().map(this::toNetworkSummary).toList(),
                documents.total(),
                documents.page(),
                documents.size());
    }

    public IidmNetworkDocument network(String networkId) {
        return networkRepository.findByField("id", networkId, 1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("IIDM network not found: " + networkId));
    }

    public IidmTableBundle networkTableMetadata(String networkId) {
        IidmNetworkDocument document = network(networkId);
        return tableBundle(document, "", 0, 0, true);
    }

    public IidmTableBundle networkTablesForFile(String importId, String fileId) {
        IidmProfileTransformDocument transform = transformByFileId(fileId);
        if (!transform.importId().equals(importId)) {
            throw new IllegalArgumentException("IIDM transform not found for import/file: " + importId + "/" + fileId);
        }
        return networkTableMetadata(transform.iidmNetworkId());
    }

    public IidmTableBundle networkTable(String networkId, String tableId, int page, int size, String search) {
        IidmNetworkDocument document = network(networkId);
        return tableBundle(document, tableId, page, size, false, search);
    }

    public IidmTableBundle gridViewTableMetadata(String networkId) {
        IidmNetworkDocument document = network(networkId);
        return gridViewTableBundle(document, "grid-summary", 0, 0, true, "");
    }

    public IidmTableBundle gridViewTable(String networkId, String tableId, int page, int size, String search) {
        IidmNetworkDocument document = network(networkId);
        return gridViewTableBundle(document, tableId, page, size, false, search);
    }

    public IidmGridViewMapResponse gridViewMap(String networkId, boolean regenerate) {
        IidmNetworkDocument network = network(networkId);
        String mapId = network.id();
        if (!regenerate) {
            List<IidmGridViewDocument> existing = gridViewRepository.findByField("id", mapId, 1);
            if (!existing.isEmpty() && "DONE".equals(existing.getFirst().state())) {
                try {
                    String svg = new String(objectStorageService.read(existing.getFirst().bucket(), existing.getFirst().objectKey()), StandardCharsets.UTF_8);
                    return toGridViewMapResponse(existing.getFirst(), svg);
                } catch (RuntimeException exception) {
                    logger.warn("Stored Grid View map object is not readable for network {}; regenerating", networkId, exception);
                }
            }
        }

        long startedAt = Instant.now().toEpochMilli();
        String objectKey = network.importId() + "/" + safeObjectName(network.id()) + "/grid-view.svg";
        IidmGridViewDocument started = new IidmGridViewDocument(
                mapId,
                network.importId(),
                network.id(),
                gridViewBucket,
                objectKey,
                "image/svg+xml",
                "STARTED",
                network.sourceFileIds(),
                0,
                0,
                0,
                List.of("Grid View map generation started"),
                startedAt,
                startedAt);
        gridViewRepository.save(started);

        try {
            GridViewSvg svg = buildGridViewSvg(network);
            objectStorageService.store(gridViewBucket, objectKey, svg.svg().getBytes(StandardCharsets.UTF_8), "image/svg+xml");
            long now = Instant.now().toEpochMilli();
            IidmGridViewDocument done = new IidmGridViewDocument(
                    mapId,
                    network.importId(),
                    network.id(),
                    gridViewBucket,
                    objectKey,
                    "image/svg+xml",
                    "DONE",
                    network.sourceFileIds(),
                    svg.coordinateCount(),
                    svg.lineCount(),
                    svg.substationCount(),
                    svg.diagnostics(),
                    now,
                    now);
            gridViewRepository.save(done);
            return toGridViewMapResponse(done, svg.svg());
        } catch (RuntimeException exception) {
            long now = Instant.now().toEpochMilli();
            IidmGridViewDocument failed = new IidmGridViewDocument(
                    mapId,
                    network.importId(),
                    network.id(),
                    gridViewBucket,
                    objectKey,
                    "image/svg+xml",
                    "FAILED",
                    network.sourceFileIds(),
                    0,
                    0,
                    0,
                    List.of(exception.getClass().getSimpleName() + ": " + exception.getMessage()),
                    startedAt,
                    now);
            gridViewRepository.save(failed);
            throw exception;
        }
    }

    public IidmGridViewMapDataResponse gridViewMapData(String networkId) {
        IidmNetworkDocument document = network(networkId);
        List<Map<String, Object>> locationRows = glProfileRows(document, "locations");
        List<GridPoint> glPoints = glProfileRows(document, "positionPoints").stream()
                .map(this::toGridPoint)
                .filter(GridPoint::valid)
                .sorted(Comparator.comparing(GridPoint::locationId).thenComparingInt(GridPoint::sequenceNumber))
                .toList();
        Map<String, Map<String, Object>> locationsById = glLocationIndex(locationRows);
        Map<String, List<GridPoint>> pointsByLocation = new LinkedHashMap<>();
        for (GridPoint point : glPoints) {
            String key = point.locationId().isBlank() ? point.rowId() : canonicalId(point.locationId());
            pointsByLocation.computeIfAbsent(key, ignored -> new ArrayList<>()).add(point);
        }

        List<GridViewPoint> points = new ArrayList<>();
        List<GridViewLine> lines = new ArrayList<>();
        for (Map.Entry<String, List<GridPoint>> entry : pointsByLocation.entrySet()) {
            List<GridPoint> groupedPoints = entry.getValue();
            if (groupedPoints.isEmpty()) {
                continue;
            }
            Map<String, Object> location = locationsById.getOrDefault(entry.getKey(), Map.of());
            String label = gridLocationLabel(location, groupedPoints.getFirst());
            List<GridViewPoint> linePoints = groupedPoints.stream()
                    .map(point -> gridViewPoint(point, gridPointLabel(location, point), location))
                    .toList();
            if (linePoints.size() > 1) {
                lines.add(new GridViewLine(entry.getKey(), label, linePoints, gridViewDetails(location, groupedPoints.getFirst())));
            }
            points.add(gridViewPoint(groupedPoints.getFirst(), label, location));
        }

        List<String> diagnostics = new ArrayList<>();
        diagnostics.add("Grid View data generated from stored GL profile coordinates");
        diagnostics.add("GL location rows found: " + locationRows.size());
        diagnostics.add("GL position points found: " + glPoints.size());
        diagnostics.add("Map markers generated: " + points.size());
        diagnostics.add("Map lines generated: " + lines.size());
        return new IidmGridViewMapDataResponse(
                document.importId(),
                document.id(),
                document.tsoName(),
                document.businessDay(),
                document.businessTime(),
                document.timeFrame(),
                gridViewBounds(glPoints),
                points,
                lines,
                diagnostics);
    }

    private CnmProfilePayloadReadDocument sourcePayload(String payloadId, String importId) {
        return sourcePayloadRepository.findByField("id", payloadId, 1)
                .stream()
                .filter(payload -> payload.importId().equals(importId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("CNM profile payload not found: " + payloadId));
    }

    private CnmNetworkSnapshotReadDocument sourceSnapshot(String snapshotId, String importId) {
        return sourceSnapshotRepository.findByField("id", snapshotId, 1)
                .stream()
                .filter(snapshot -> snapshot.importId().equals(importId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("CNM network snapshot not found: " + snapshotId));
    }

    private CgmNetworkSnapshot reconstructSnapshot(CnmNetworkSnapshotReadDocument sourceSnapshot) {
        int expectedPayloadSections = number(sourceSnapshot.payloadSectionCount());
        List<CnmNetworkSnapshotPayloadReadDocument> payloads = sourceSnapshotPayloadRepository
                .findByField("snapshotId", sourceSnapshot.id(), Math.max(50_000, expectedPayloadSections + 10))
                .stream()
                .filter(payload -> payload.importId().equals(sourceSnapshot.importId()))
                .sorted((left, right) -> {
                    int section = left.section().compareTo(right.section());
                    return section != 0 ? section : Integer.compare(number(left.sequence()), number(right.sequence()));
                })
                .toList();
        if (payloads.isEmpty()) {
            throw new IllegalArgumentException("CNM network snapshot payload not found: " + sourceSnapshot.id());
        }
        if (expectedPayloadSections > 0 && payloads.size() < expectedPayloadSections) {
            throw new IllegalStateException("CNM network snapshot payload is incomplete: expected "
                    + expectedPayloadSections + " sections but found " + payloads.size());
        }
        List<GridTopologyObject> topologyObjects = sectionValues(payloads, "TOPOLOGY_OBJECTS", GridTopologyObject[].class);
        List<GridTopologyRelation> topologyRelations = sectionValues(payloads, "TOPOLOGY_RELATIONS", GridTopologyRelation[].class);
        List<StateVariablePoint> stateValues = sectionValues(payloads, "STATE_VALUES", StateVariablePoint[].class);
        List<String> unresolvedReferences = sectionValues(payloads, "UNRESOLVED_REFERENCES", String[].class);
        List<String> diagnostics = sectionValues(payloads, "DIAGNOSTICS", String[].class);
        return new CgmNetworkSnapshot(
                sourceSnapshot.id(),
                sourceSnapshot.importId(),
                sourceSnapshot.serviceType(),
                sourceSnapshot.tsoName(),
                sourceSnapshot.businessDay(),
                sourceSnapshot.businessTime(),
                sourceSnapshot.timeFrame(),
                new StaticTopologyModel(topologyObjects, topologyRelations, unresolvedReferences),
                new StateSnapshot(
                        sourceSnapshot.id(),
                        sourceSnapshot.businessDay(),
                        sourceSnapshot.businessTime(),
                        sourceSnapshot.timeFrame(),
                        stateValues),
                sourceSnapshot.sourceFileIds(),
                diagnostics,
                List.of());
    }

    private <T> List<T> sectionValues(
            List<CnmNetworkSnapshotPayloadReadDocument> payloads,
            String section,
            Class<T[]> targetType) {
        List<T> values = new ArrayList<>();
        payloads.stream()
                .filter(payload -> section.equals(payload.section()))
                .forEach(payload -> values.addAll(List.of(jsonMappingService.fromJson(payload.payloadJson(), targetType))));
        return values;
    }

    private IidmNetworkDocument toNetworkDocument(IidmNetworkModel network) {
        removeNonSerializableCgmesImportContext(network.network());
        String networkXiidm = IidmNetworkXiidm.write(network.network());
        String networkJson = jsonMappingService.toJson(networkJsonProjection.project(network.network()));
        String xiidmObjectKey = networkPayloadObjectKey(network.importId(), network.id(), "network.xiidm");
        String jsonObjectKey = networkPayloadObjectKey(network.importId(), network.id(), "network-projection.json");
        objectStorageService.store(networkPayloadBucket, xiidmObjectKey, networkXiidm.getBytes(StandardCharsets.UTF_8), "application/xml");
        objectStorageService.store(networkPayloadBucket, jsonObjectKey, networkJson.getBytes(StandardCharsets.UTF_8), "application/json");
        IidmNetworkSummary summary = network.summary();
        long now = Instant.now().toEpochMilli();
        return new IidmNetworkDocument(
                network.id(),
                network.importId(),
                network.sourceFileIds(),
                network.businessDay(),
                network.businessTime(),
                network.timeFrame(),
                network.tsoName(),
                IidmNetworkXiidm.FORMAT,
                "",
                List.of(),
                networkPayloadBucket,
                xiidmObjectKey,
                sha256(networkXiidm),
                (long) networkXiidm.getBytes(StandardCharsets.UTF_8).length,
                IidmNetworkJsonProjection.TYPE,
                "",
                List.of(),
                networkPayloadBucket,
                jsonObjectKey,
                sha256(networkJson),
                (long) networkJson.getBytes(StandardCharsets.UTF_8).length,
                List.of(
                        new IidmElementCountDocument("substations", summary.substationCount()),
                        new IidmElementCountDocument("voltageLevels", summary.voltageLevelCount()),
                        new IidmElementCountDocument("buses", summary.busCount()),
                        new IidmElementCountDocument("lines", summary.lineCount()),
                        new IidmElementCountDocument("generators", summary.generatorCount()),
                        new IidmElementCountDocument("loads", summary.loadCount()),
                        new IidmElementCountDocument("switches", summary.switchCount()),
                        new IidmElementCountDocument("busbarSections", summary.busbarSectionCount())),
                now,
                now);
    }

    private void removeNonSerializableCgmesImportContext(Network network) {
        if (network.removeExtension(CgmesModelExtension.class)) {
            logger.debug(
                    "Removed non-serializable PowSyBl CGMES import context extension before XIIDM persistence for network {}",
                    network.getId());
        }
    }

    private String profileJson(CnmProfilePayloadReadDocument payload) {
        if (payload.profileJson() != null && !payload.profileJson().isBlank()) {
            return payload.profileJson();
        }
        if (hasObjectPayload(payload.payloadBucket(), payload.payloadObjectKey())) {
            return readObjectPayload(payload.payloadBucket(), payload.payloadObjectKey());
        }
        if (payload.profileJsonChunks() != null && !payload.profileJsonChunks().isEmpty()) {
            return String.join("", payload.profileJsonChunks());
        }
        return sourcePayloadChunkRepository.findByField("fileId", payload.fileId(), 10_000)
                .stream()
                .filter(chunk -> payload.importId().equals(chunk.importId()))
                .sorted((left, right) -> Integer.compare(number(left.chunkIndex()), number(right.chunkIndex())))
                .map(CnmProfilePayloadChunkReadDocument::chunkJson)
                .reduce("", String::concat);
    }

    private List<String> chunks(String value) {
        if (value == null || value.length() <= NETWORK_XIIDM_CHUNK_SIZE) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (int index = 0; index < value.length(); index += NETWORK_XIIDM_CHUNK_SIZE) {
            chunks.add(value.substring(index, Math.min(index + NETWORK_XIIDM_CHUNK_SIZE, value.length())));
        }
        return chunks;
    }

    private String networkId(String importId, String fileId) {
        return importId + ":" + fileId;
    }

    private String mergedNetworkId(String importId) {
        return importId + ":MERGED_CGM";
    }

    private String transformId(IidmProfileTransformRequested request) {
        String correlationKey = request.transformCorrelationKey() == null || request.transformCorrelationKey().isBlank()
                ? networkId(request.importId(), request.fileId())
                : request.transformCorrelationKey();
        return request.importId() + ":" + sha256(correlationKey);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean hasObjectPayload(String bucket, String objectKey) {
        return bucket != null && !bucket.isBlank() && objectKey != null && !objectKey.isBlank();
    }

    private String readObjectPayload(String bucket, String objectKey) {
        return new String(objectStorageService.read(bucket, objectKey), StandardCharsets.UTF_8);
    }

    private String networkPayloadObjectKey(String importId, String networkId, String suffix) {
        return safeObjectName(importId) + "/" + safeObjectName(networkId) + "/" + suffix;
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private List<DocumentFilter> filters(String importId) {
        if (importId == null || importId.isBlank()) {
            return List.of();
        }
        return List.of(DocumentFilter.exact("importId", importId));
    }

    private List<DocumentFilter> iidmTransformSearchFilters(String search) {
        if (search == null || search.isBlank()) {
            return List.of();
        }
        String query = search.trim();
        return List.of(
                DocumentFilter.contains("fileId", query),
                DocumentFilter.contains("sourceFileNames", query),
                DocumentFilter.contains("sourceFileIds", query),
                DocumentFilter.contains("profileType", query),
                DocumentFilter.contains("profileFamily", query),
                DocumentFilter.contains("transformState", query),
                DocumentFilter.contains("transformMessage", query),
                DocumentFilter.contains("iidmNetworkId", query));
    }

    private IidmTransformSummaryResponse toTransformSummary(IidmProfileTransformDocument document) {
        return new IidmTransformSummaryResponse(
                document.id(),
                document.importId(),
                document.fileId(),
                document.sourceFileIds(),
                document.sourceFileNames(),
                document.profileType(),
                document.profileFamily(),
                document.transformState(),
                document.transformMessage(),
                document.diagnostics().size(),
                document.iidmNetworkId(),
                document.startedAt(),
                document.completedAt(),
                document.failedAt());
    }

    private IidmNetworkSummaryResponse toNetworkSummary(IidmNetworkDocument document) {
        return new IidmNetworkSummaryResponse(
                document.id(),
                document.importId(),
                document.sourceFileIds(),
                document.businessDay(),
                document.businessTime(),
                document.timeFrame(),
                document.tsoName(),
                document.networkFormat(),
                document.elementCounts().stream()
                        .map(count -> new IidmElementCountResponse(count.elementType(), count.count()))
                        .toList(),
                document.createdAt(),
                document.updatedAt());
    }

    private List<String> sourceFileIds(IidmProfileTransformRequested request) {
        if (request.sourceFiles() == null || request.sourceFiles().isEmpty()) {
            return request.fileId() == null || request.fileId().isBlank() ? List.of() : List.of(request.fileId());
        }
        return request.sourceFiles().stream()
                .map(CgmesIidmSourceFile::fileId)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private List<String> sourceFileNames(IidmProfileTransformRequested request) {
        if (request.sourceFiles() == null || request.sourceFiles().isEmpty()) {
            return List.of();
        }
        return request.sourceFiles().stream()
                .map(this::sourceFileReference)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private String sourceFileReference(CgmesIidmSourceFile source) {
        if (source == null) {
            return "";
        }
        if (source.fileName() != null && !source.fileName().isBlank()) {
            return source.fileName();
        }
        return source.fileId();
    }

    private IidmTableBundle tableBundle(IidmNetworkDocument document, String selectedTableId, int page, int size, boolean metadataOnly) {
        return tableBundle(document, selectedTableId, page, size, metadataOnly, "");
    }

    private IidmTableBundle tableBundle(
            IidmNetworkDocument document,
            String selectedTableId,
            int page,
            int size,
            boolean metadataOnly,
            String search) {
        int resolvedSize = metadataOnly ? 0 : Math.min(Math.max(size, 1), 500);
        int resolvedPage = Math.max(page, 0);
        List<IidmTableSpec> specs = metadataOnly
                ? iidmTableMetadataSpecs(document)
                : iidmTableSpecs(document, selectedTableId);
        List<DynamicTableDefinition> tables = specs.stream()
                .map(spec -> {
                    if (metadataOnly || !spec.tableId().equals(selectedTableId)) {
                        return new DynamicTableDefinition(
                                spec.tableId(),
                                spec.label(),
                                spec.columns(),
                                List.of(),
                                spec.totalRows(),
                                spec.defaultSort());
                    }
                    List<Map<String, Object>> rows = spec.rows().stream()
                            .filter(row -> matches(row, search))
                            .toList();
                    return table(spec.tableId(), spec.label(), spec.columns(), rows, resolvedPage, resolvedSize, spec.defaultSort());
                })
                .toList();
        return new IidmTableBundle(
                document.importId(),
                document.id(),
                document.sourceFileIds().isEmpty() ? "" : document.sourceFileIds().getFirst(),
                selectedTableId,
                resolvedPage,
                resolvedSize,
                tables);
    }

    private List<IidmTableSpec> iidmTableSpecs(IidmNetworkDocument document, String selectedTableId) {
        IidmTableSpec selectedSpec = iidmSelectedTableSpec(document, selectedTableId);
        return iidmTableMetadataSpecs(document).stream()
                .map(spec -> spec.tableId().equals(selectedTableId) ? selectedSpec : spec)
                .toList();
    }

    private IidmTableSpec iidmSelectedTableSpec(IidmNetworkDocument document, String selectedTableId) {
        return switch (selectedTableId) {
            case "network-summary", "element-counts", "source-files" -> iidmTableDataSpecs(document).stream()
                    .filter(spec -> spec.tableId().equals(selectedTableId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown IIDM table: " + selectedTableId));
            case "substations" -> withProjectedRows(document, "substations", "Substations", substationColumns(),
                    network -> stream(network.getSubstations()).stream().map(this::substationRow).toList());
            case "voltage-levels" -> withProjectedRows(document, "voltage-levels", "Voltage levels", voltageLevelColumns(),
                    network -> stream(network.getVoltageLevels()).stream().map(this::voltageLevelRow).toList());
            case "buses" -> withProjectedRows(document, "buses", "Buses", busColumns(),
                    network -> stream(network.getBusBreakerView().getBuses()).stream().map(this::busRow).toList());
            case "lines" -> withProjectedRows(document, "lines", "Lines", lineColumns(),
                    network -> stream(network.getLines()).stream().map(this::lineRow).toList());
            case "generators" -> withProjectedRows(document, "generators", "Generators", generatorColumns(),
                    network -> stream(network.getGenerators()).stream().map(this::generatorRow).toList());
            case "loads" -> withProjectedRows(document, "loads", "Loads", loadColumns(),
                    network -> stream(network.getLoads()).stream().map(this::loadRow).toList());
            case "switches" -> withProjectedRows(document, "switches", "Switches", switchColumns(),
                    network -> stream(network.getSwitches()).stream().map(this::switchRow).toList());
            case "busbar-sections" -> withProjectedRows(document, "busbar-sections", "Busbar sections", busbarSectionColumns(),
                    network -> stream(network.getBusbarSections()).stream().map(this::busbarSectionRow).toList());
            default -> throw new IllegalArgumentException("Unknown IIDM table: " + selectedTableId);
        };
    }

    private IidmTableSpec withProjectedRows(
            IidmNetworkDocument document,
            String tableId,
            String label,
            List<DynamicTableColumn> columns,
            Function<Network, List<Map<String, Object>>> fallbackRows) {
        List<Map<String, Object>> rows = projectedRows(document, tableId);
        if (rows != null) {
            return new IidmTableSpec(tableId, label, columns, rows, "id");
        }
        return withNetwork(document, tableId, label, columns, fallbackRows);
    }

    private IidmTableSpec withNetwork(
            IidmNetworkDocument document,
            String tableId,
            String label,
            List<DynamicTableColumn> columns,
            Function<Network, List<Map<String, Object>>> rows) {
        return new IidmTableSpec(tableId, label, columns, rows.apply(IidmNetworkXiidm.read(networkXiidm(document))), "id");
    }

    private List<IidmTableSpec> iidmTableMetadataSpecs(IidmNetworkDocument document) {
        List<IidmTableSpec> specs = iidmTableDataSpecs(document).stream()
                .map(spec -> spec.withoutRows())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        specs.add(new IidmTableSpec("substations", "Substations", substationColumns(), List.of(), "id", count(document, "substations")));
        specs.add(new IidmTableSpec("voltage-levels", "Voltage levels", voltageLevelColumns(), List.of(), "id", count(document, "voltageLevels")));
        specs.add(new IidmTableSpec("buses", "Buses", busColumns(), List.of(), "id", count(document, "buses")));
        specs.add(new IidmTableSpec("lines", "Lines", lineColumns(), List.of(), "id", count(document, "lines")));
        specs.add(new IidmTableSpec("generators", "Generators", generatorColumns(), List.of(), "id", count(document, "generators")));
        specs.add(new IidmTableSpec("loads", "Loads", loadColumns(), List.of(), "id", count(document, "loads")));
        specs.add(new IidmTableSpec("switches", "Switches", switchColumns(), List.of(), "id", count(document, "switches")));
        specs.add(new IidmTableSpec("busbar-sections", "Busbar sections", busbarSectionColumns(), List.of(), "id", count(document, "busbarSections")));
        return specs;
    }

    private IidmTableBundle gridViewTableBundle(
            IidmNetworkDocument document,
            String selectedTableId,
            int page,
            int size,
            boolean metadataOnly,
            String search) {
        int resolvedSize = metadataOnly ? 0 : Math.min(Math.max(size, 1), 500);
        int resolvedPage = Math.max(page, 0);
        List<IidmTableSpec> specs = metadataOnly
                ? gridViewMetadataSpecs(document)
                : gridViewTableSpecs(document, selectedTableId);
        List<DynamicTableDefinition> tables = specs.stream()
                .map(spec -> {
                    if (metadataOnly || !spec.tableId().equals(selectedTableId)) {
                        return new DynamicTableDefinition(
                                spec.tableId(),
                                spec.label(),
                                spec.columns(),
                                List.of(),
                                spec.totalRows(),
                                spec.defaultSort());
                    }
                    List<Map<String, Object>> rows = spec.rows().stream()
                            .filter(row -> matches(row, search))
                            .toList();
                    return table(spec.tableId(), spec.label(), spec.columns(), rows, resolvedPage, resolvedSize, spec.defaultSort());
                })
                .toList();
        return new IidmTableBundle(
                document.importId(),
                document.id(),
                document.sourceFileIds().isEmpty() ? "" : document.sourceFileIds().getFirst(),
                selectedTableId,
                resolvedPage,
                resolvedSize,
                tables);
    }

    private List<IidmTableSpec> gridViewTableSpecs(IidmNetworkDocument document, String selectedTableId) {
        IidmTableSpec selectedSpec = gridViewSelectedTableSpec(document, selectedTableId);
        return gridViewMetadataSpecs(document).stream()
                .map(spec -> spec.tableId().equals(selectedTableId) ? selectedSpec : spec)
                .toList();
    }

    private IidmTableSpec gridViewSelectedTableSpec(IidmNetworkDocument document, String selectedTableId) {
        return switch (selectedTableId) {
            case "grid-summary" -> gridViewSummarySpec(document);
            case "substation-positions" -> new IidmTableSpec(
                    "substation-positions",
                    "Substation positions",
                    positionColumns(),
                    projectedRowsOrEmpty(document, "substation-positions"),
                    "id");
            case "line-positions" -> new IidmTableSpec(
                    "line-positions",
                    "Line positions",
                    positionColumns(),
                    projectedRowsOrEmpty(document, "line-positions"),
                    "id");
            case "iidm-positions" -> new IidmTableSpec(
                    "iidm-positions",
                    "IIDM positions",
                    positionColumns(),
                    projectedRowsOrEmpty(document, "iidm-positions"),
                    "id");
            case "gl-locations" -> new IidmTableSpec(
                    "gl-locations",
                    "GL locations",
                    columns("mRID", "mRID", "canonicalMRID", "Canonical mRID", "name", "Name", "type", "Type",
                            "powerSystemResourceId", "Grid element", "canonicalPowerSystemResource", "Canonical grid element",
                            "coordinateSystemId", "Coordinate system", "sourceFileId", "Source file"),
                    glProfileRows(document, "locations"),
                    "mRID");
            case "gl-position-points" -> new IidmTableSpec(
                    "gl-position-points",
                    "GL position points",
                    columns("mRID", "mRID", "locationId", "Location", "sequenceNumber", "Sequence",
                            "xPosition", "Longitude/X", "yPosition", "Latitude/Y", "zPosition", "Z", "sourceFileId", "Source file"),
                    glProfileRows(document, "positionPoints"),
                    "locationId");
            case "gl-coordinate-systems" -> new IidmTableSpec(
                    "gl-coordinate-systems",
                    "GL coordinate systems",
                    columns("mRID", "mRID", "name", "Name", "type", "Type", "sourceFileId", "Source file"),
                    glProfileRows(document, "coordinateSystems"),
                    "mRID");
            default -> throw new IllegalArgumentException("Unknown Grid View table: " + selectedTableId);
        };
    }

    private List<IidmTableSpec> gridViewMetadataSpecs(IidmNetworkDocument document) {
        List<IidmTableSpec> specs = new ArrayList<>();
        specs.add(gridViewSummarySpec(document).withoutRows());
        specs.add(new IidmTableSpec("substation-positions", "Substation positions", positionColumns(), List.of(), "id",
                projectedRowsOrEmpty(document, "substation-positions").size()));
        specs.add(new IidmTableSpec("line-positions", "Line positions", positionColumns(), List.of(), "id",
                projectedRowsOrEmpty(document, "line-positions").size()));
        specs.add(new IidmTableSpec("iidm-positions", "IIDM positions", positionColumns(), List.of(), "id",
                projectedRowsOrEmpty(document, "iidm-positions").size()));
        specs.add(new IidmTableSpec("gl-locations", "GL locations",
                columns("mRID", "mRID", "canonicalMRID", "Canonical mRID", "name", "Name", "type", "Type",
                        "powerSystemResourceId", "Grid element", "canonicalPowerSystemResource", "Canonical grid element",
                        "coordinateSystemId", "Coordinate system", "sourceFileId", "Source file"),
                List.of(), "mRID", glProfileRows(document, "locations").size()));
        specs.add(new IidmTableSpec("gl-position-points", "GL position points",
                columns("mRID", "mRID", "locationId", "Location", "sequenceNumber", "Sequence",
                        "xPosition", "Longitude/X", "yPosition", "Latitude/Y", "zPosition", "Z", "sourceFileId", "Source file"),
                List.of(), "locationId", glProfileRows(document, "positionPoints").size()));
        specs.add(new IidmTableSpec("gl-coordinate-systems", "GL coordinate systems",
                columns("mRID", "mRID", "name", "Name", "type", "Type", "sourceFileId", "Source file"),
                List.of(), "mRID", glProfileRows(document, "coordinateSystems").size()));
        return specs;
    }

    private IidmTableSpec gridViewSummarySpec(IidmNetworkDocument document) {
        List<Map<String, Object>> rows = List.of(
                row("Network ID", document.id()),
                row("Import ID", document.importId()),
                row("Source files", String.join(", ", document.sourceFileIds())),
                row("TSO", document.tsoName()),
                row("Business day", document.businessDay()),
                row("Business time", document.businessTime()),
                row("Timeframe", document.timeFrame()),
                row("IIDM substation positions", projectedRowsOrEmpty(document, "substation-positions").size()),
                row("IIDM line positions", projectedRowsOrEmpty(document, "line-positions").size()),
                row("IIDM positions", projectedRowsOrEmpty(document, "iidm-positions").size()),
                row("GL locations", glProfileRows(document, "locations").size()),
                row("GL position points", glProfileRows(document, "positionPoints").size()),
                row("GL coordinate systems", glProfileRows(document, "coordinateSystems").size()));
        return new IidmTableSpec("grid-summary", "Grid summary", columns("field", "Field", "value", "Value"), rows, "field");
    }

    private List<IidmTableSpec> iidmTableDataSpecs(IidmNetworkDocument document) {
        List<IidmTableSpec> specs = new ArrayList<>();
        specs.add(new IidmTableSpec(
                "network-summary",
                "Network summary",
                columns("field", "Field", "value", "Value"),
                List.of(
                        row("Network ID", document.id()),
                        row("Import ID", document.importId()),
                        row("Source files", String.join(", ", document.sourceFileIds())),
                        row("TSO", document.tsoName()),
                        row("Business day", document.businessDay()),
                        row("Business time", document.businessTime()),
                        row("Timeframe", document.timeFrame()),
                        row("Format", document.networkFormat())),
                "field"));
        specs.add(new IidmTableSpec(
                "element-counts",
                "Element counts",
                columns("elementType", "Element type", "count", "Count"),
                document.elementCounts().stream()
                        .map(count -> map(
                                "elementType", count.elementType(),
                                "count", count.count()))
                        .toList(),
                "elementType"));
        specs.add(new IidmTableSpec(
                "source-files",
                "Source files",
                columns("fileId", "File ID"),
                document.sourceFileIds().stream().map(fileId -> map("fileId", fileId)).toList(),
                "fileId"));
        return specs;
    }

    private int count(IidmNetworkDocument document, String elementType) {
        return document.elementCounts().stream()
                .filter(count -> elementType.equals(count.elementType()))
                .mapToInt(count -> Math.toIntExact(count.count()))
                .findFirst()
                .orElse(0);
    }

    private String networkXiidm(IidmNetworkDocument document) {
        if (document.networkXiidm() != null && !document.networkXiidm().isBlank()) {
            return document.networkXiidm();
        }
        if (hasObjectPayload(document.networkXiidmBucket(), document.networkXiidmObjectKey())) {
            return readObjectPayload(document.networkXiidmBucket(), document.networkXiidmObjectKey());
        }
        return String.join("", document.networkXiidmChunks());
    }

    private List<Map<String, Object>> projectedRows(IidmNetworkDocument document, String tableId) {
        String json = networkJson(document);
        if (json.isBlank()) {
            return null;
        }
        Map<?, ?> projection = jsonMappingService.fromJson(json, Map.class);
        Object value = projection.get(tableId);
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object row : values) {
            if (row instanceof Map<?, ?> rowMap) {
                rows.add(toStringKeyMap(rowMap));
            }
        }
        return rows;
    }

    private List<Map<String, Object>> projectedRowsOrEmpty(IidmNetworkDocument document, String tableId) {
        List<Map<String, Object>> rows = projectedRows(document, tableId);
        return rows == null ? List.of() : rows;
    }

    private List<Map<String, Object>> glProfileRows(IidmNetworkDocument document, String section) {
        List<CnmProfilePayloadReadDocument> payloads = glProfilePayloads(document);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CnmProfilePayloadReadDocument payload : payloads) {
            String json = profileJson(payload);
            if (json.isBlank()) {
                continue;
            }
            Map<?, ?> root = jsonMappingService.fromJson(json, Map.class);
            Object profile = root.get("profile");
            if (!(profile instanceof Map<?, ?> profileMap)) {
                continue;
            }
            Object sectionValue = profileMap.get(section);
            if (!(sectionValue instanceof List<?> values)) {
                continue;
            }
            for (Object value : values) {
                if (value instanceof Map<?, ?> map) {
                    Map<String, Object> row = toStringKeyMap(map);
                    row.put("sourceFileId", payload.fileId());
                    enrichCanonicalGlRow(section, row);
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void enrichCanonicalGlRow(String section, Map<String, Object> row) {
        Object mRID = row.get("mRID");
        if (mRID != null) {
            row.putIfAbsent("canonicalMRID", canonicalId(String.valueOf(mRID)));
        }
        if ("locations".equals(section)) {
            Object resource = row.get("powerSystemResourceId");
            if (resource != null) {
                row.putIfAbsent("canonicalPowerSystemResource", canonicalId(String.valueOf(resource)));
            }
            Object coordinateSystem = row.get("coordinateSystemId");
            if (coordinateSystem != null) {
                row.putIfAbsent("canonicalCoordinateSystem", canonicalId(String.valueOf(coordinateSystem)));
            }
        }
        if ("positionPoints".equals(section)) {
            Object location = row.get("locationId");
            if (location != null) {
                row.putIfAbsent("canonicalLocation", canonicalId(String.valueOf(location)));
            }
        }
    }

    private List<CnmProfilePayloadReadDocument> glProfilePayloads(IidmNetworkDocument document) {
        return sourceProfileRepository.findByField("importId", document.importId(), 1_000)
                .stream()
                .filter(profile -> isMatchingGridViewProfile(document, profile))
                .flatMap(profile -> sourcePayloadRepository.findByField("fileId", profile.fileId(), 1).stream())
                .filter(this::isGeographicalLocationPayload)
                .toList();
    }

    private boolean isMatchingGridViewProfile(IidmNetworkDocument document, CnmProfileReadDocument profile) {
        return profile != null
                && isGeographicalLocationProfile(profile)
                && sameValue(profile.tsoName(), document.tsoName())
                && sameValue(profile.businessDay(), document.businessDay());
    }

    private boolean isGeographicalLocationProfile(CnmProfileReadDocument profile) {
        String type = valueOr(profile.profileType(), "");
        String kind = valueOr(profile.detectedProfileKind(), "");
        String jsonType = valueOr(profile.profileJsonType(), "");
        return "GL".equalsIgnoreCase(type)
                || "GEOGRAPHICAL_LOCATION".equalsIgnoreCase(kind)
                || jsonType.contains("GEOGRAPHICAL_LOCATION")
                || jsonType.endsWith("_GL");
    }

    private boolean sameValue(String left, String right) {
        return valueOr(left, "").equalsIgnoreCase(valueOr(right, ""));
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private GridViewSvg buildGridViewSvg(IidmNetworkDocument document) {
        List<GridPoint> points = glProfileRows(document, "positionPoints").stream()
                .map(this::toGridPoint)
                .filter(GridPoint::valid)
                .sorted((left, right) -> {
                    int locationCompare = left.locationId().compareTo(right.locationId());
                    return locationCompare != 0 ? locationCompare : Integer.compare(left.sequenceNumber(), right.sequenceNumber());
                })
                .toList();
        List<Map<String, Object>> substationPositions = projectedRowsOrEmpty(document, "substation-positions");
        List<Map<String, Object>> iidmPositions = projectedRowsOrEmpty(document, "iidm-positions");
        List<String> diagnostics = new ArrayList<>();
        diagnostics.add("Grid View generated from stored GL profile coordinates");
        diagnostics.add("IIDM position extensions found: " + iidmPositions.size());
        diagnostics.add("IIDM substation position extensions found: " + substationPositions.size());
        if (points.isEmpty()) {
            diagnostics.add("No GL position points were available for this IIDM network");
        }
        Map<String, List<GridPoint>> byLocation = new LinkedHashMap<>();
        for (GridPoint point : points) {
            byLocation.computeIfAbsent(point.locationId().isBlank() ? point.rowId() : point.locationId(), ignored -> new ArrayList<>()).add(point);
        }
        String svg = renderWorldMapSvg(document, byLocation, diagnostics);
        long lineCount = byLocation.values().stream().filter(value -> value.size() > 1).count();
        return new GridViewSvg(svg, points.size(), Math.toIntExact(lineCount), substationPositions.size(), diagnostics);
    }

    private String renderWorldMapSvg(IidmNetworkDocument document, Map<String, List<GridPoint>> locations, List<String> diagnostics) {
        int width = 1200;
        int height = 680;
        int left = 60;
        int top = 40;
        int mapWidth = 1080;
        int mapHeight = 540;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width).append(' ').append(height).append("\">");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#08111f\"/>");
        svg.append("<rect x=\"").append(left).append("\" y=\"").append(top).append("\" width=\"").append(mapWidth)
                .append("\" height=\"").append(mapHeight).append("\" rx=\"14\" fill=\"#0f2035\" stroke=\"#274968\"/>");
        for (int lon = -180; lon <= 180; lon += 30) {
            double x = left + ((lon + 180.0) / 360.0) * mapWidth;
            svg.append("<line x1=\"").append(x).append("\" y1=\"").append(top).append("\" x2=\"").append(x)
                    .append("\" y2=\"").append(top + mapHeight).append("\" stroke=\"#20384f\" stroke-width=\"1\"/>");
        }
        for (int lat = -60; lat <= 60; lat += 30) {
            double y = top + ((90.0 - lat) / 180.0) * mapHeight;
            svg.append("<line x1=\"").append(left).append("\" y1=\"").append(y).append("\" x2=\"").append(left + mapWidth)
                    .append("\" y2=\"").append(y).append("\" stroke=\"#20384f\" stroke-width=\"1\"/>");
        }
        svg.append("<text x=\"60\" y=\"620\" fill=\"#b7c9dc\" font-family=\"Inter, Arial\" font-size=\"18\">")
                .append(escapeXml(document.tsoName())).append(" Grid View - ").append(escapeXml(document.businessDay()))
                .append(' ').append(escapeXml(document.businessTime())).append("</text>");
        svg.append("<text x=\"60\" y=\"648\" fill=\"#7f98b3\" font-family=\"Inter, Arial\" font-size=\"14\">")
                .append(escapeXml(document.id())).append("</text>");

        int colorIndex = 0;
        String[] colors = {"#00f2fe", "#b026ff", "#22c55e", "#f59e0b", "#fb7185"};
        for (List<GridPoint> points : locations.values()) {
            if (points.isEmpty()) {
                continue;
            }
            String color = colors[colorIndex++ % colors.length];
            if (points.size() > 1) {
                svg.append("<polyline fill=\"none\" stroke=\"").append(color)
                        .append("\" stroke-width=\"2\" stroke-linejoin=\"round\" stroke-linecap=\"round\" points=\"");
                for (GridPoint point : points) {
                    svg.append(projectX(point.longitude(), left, mapWidth)).append(',').append(projectY(point.latitude(), top, mapHeight)).append(' ');
                }
                svg.append("\"/>");
            }
            for (GridPoint point : points) {
                double x = projectX(point.longitude(), left, mapWidth);
                double y = projectY(point.latitude(), top, mapHeight);
                svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
                        .append("\" r=\"4\" fill=\"").append(color).append("\" stroke=\"#ffffff\" stroke-width=\"1\">")
                        .append("<title>").append(escapeXml(point.label())).append("</title></circle>");
            }
        }
        if (locations.isEmpty()) {
            svg.append("<text x=\"").append(left + 36).append("\" y=\"").append(top + 80)
                    .append("\" fill=\"#dbeafe\" font-family=\"Inter, Arial\" font-size=\"24\">No GL coordinates available</text>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private GridPoint toGridPoint(Map<String, Object> row) {
        String rowId = String.valueOf(row.getOrDefault("mRID", row.getOrDefault("rowId", "")));
        String locationId = String.valueOf(row.getOrDefault("locationId", ""));
        double longitude = coordinate(row.get("xPosition"));
        double latitude = coordinate(row.get("yPosition"));
        int sequence = integer(row.get("sequenceNumber"));
        String sourceFileId = String.valueOf(row.getOrDefault("sourceFileId", ""));
        return new GridPoint(rowId, locationId, sequence, latitude, longitude, sourceFileId);
    }

    private Map<String, Map<String, Object>> glLocationIndex(List<Map<String, Object>> locations) {
        Map<String, Map<String, Object>> index = new LinkedHashMap<>();
        for (Map<String, Object> location : locations) {
            putLocationIndex(index, location, location.get("canonicalMRID"));
            putLocationIndex(index, location, location.get("mRID"));
        }
        return index;
    }

    private void putLocationIndex(Map<String, Map<String, Object>> index, Map<String, Object> location, Object key) {
        String canonical = canonicalId(String.valueOf(key == null ? "" : key));
        if (!canonical.isBlank()) {
            index.putIfAbsent(canonical, location);
        }
    }

    private GridViewPoint gridViewPoint(GridPoint point, String label, Map<String, Object> location) {
        return new GridViewPoint(point.rowId(), label, point.latitude(), point.longitude(), gridViewDetails(location, point));
    }

    private Map<String, Object> gridViewDetails(Map<String, Object> location, GridPoint point) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (location != null) {
            details.putAll(location);
        }
        details.put("positionPointId", point.rowId());
        details.put("locationId", point.locationId());
        details.put("canonicalLocationId", canonicalId(point.locationId()));
        details.put("sequenceNumber", point.sequenceNumber());
        details.put("latitude", point.latitude());
        details.put("longitude", point.longitude());
        details.put("sourceFileId", point.sourceFileId());
        return details;
    }

    private String gridLocationLabel(Map<String, Object> location, GridPoint fallback) {
        String name = location == null ? "" : stringValue(location.get("name"));
        if (!name.isBlank()) {
            return name;
        }
        String mRID = location == null ? "" : stringValue(location.get("mRID"));
        if (!mRID.isBlank()) {
            return mRID;
        }
        return fallback.rowId();
    }

    private String gridPointLabel(Map<String, Object> location, GridPoint point) {
        String base = gridLocationLabel(location, point);
        if (point.sequenceNumber() <= 0) {
            return base;
        }
        return base + " #" + point.sequenceNumber();
    }

    private GridViewBounds gridViewBounds(List<GridPoint> points) {
        if (points.isEmpty()) {
            return new GridViewBounds(34.0, 72.0, -25.0, 45.0);
        }
        double minLatitude = 90.0;
        double maxLatitude = -90.0;
        double minLongitude = 180.0;
        double maxLongitude = -180.0;
        for (GridPoint point : points) {
            minLatitude = Math.min(minLatitude, point.latitude());
            maxLatitude = Math.max(maxLatitude, point.latitude());
            minLongitude = Math.min(minLongitude, point.longitude());
            maxLongitude = Math.max(maxLongitude, point.longitude());
        }
        double latitudePadding = Math.max((maxLatitude - minLatitude) * 0.12, 0.15);
        double longitudePadding = Math.max((maxLongitude - minLongitude) * 0.12, 0.15);
        return new GridViewBounds(
                Math.max(-85.0, minLatitude - latitudePadding),
                Math.min(85.0, maxLatitude + latitudePadding),
                Math.max(-180.0, minLongitude - longitudePadding),
                Math.min(180.0, maxLongitude + longitudePadding));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double projectX(double longitude, int left, int width) {
        return left + ((Math.max(-180.0, Math.min(180.0, longitude)) + 180.0) / 360.0) * width;
    }

    private double projectY(double latitude, int top, int height) {
        return top + ((90.0 - Math.max(-90.0, Math.min(90.0, latitude))) / 180.0) * height;
    }

    private double coordinate(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (RuntimeException exception) {
            return Double.NaN;
        }
    }

    private int integer(Object value) {
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private String safeObjectName(String value) {
        return value == null ? "network" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private IidmGridViewMapResponse toGridViewMapResponse(IidmGridViewDocument document, String svg) {
        return new IidmGridViewMapResponse(
                document.id(),
                document.importId(),
                document.networkId(),
                document.bucket(),
                document.objectKey(),
                document.contentType(),
                document.state(),
                svg,
                document.coordinateCount(),
                document.lineCount(),
                document.substationCount(),
                document.diagnostics(),
                document.generatedAt());
    }

    private boolean isGeographicalLocationPayload(CnmProfilePayloadReadDocument payload) {
        String type = payload.profileJsonType() == null ? "" : payload.profileJsonType();
        String normalized = type.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        return normalized.contains("GEOGRAPHICAL_LOCATION")
                || normalized.endsWith("_GL")
                || normalized.equals("CGMES_GL")
                || normalized.equals("GL");
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        Map<String, Object> row = new LinkedHashMap<>();
        source.forEach((key, value) -> row.put(String.valueOf(key), displayValue(value)));
        return row;
    }

    private String networkJson(IidmNetworkDocument document) {
        if (document.networkJson() != null && !document.networkJson().isBlank()) {
            return document.networkJson();
        }
        if (hasObjectPayload(document.networkJsonBucket(), document.networkJsonObjectKey())) {
            return readObjectPayload(document.networkJsonBucket(), document.networkJsonObjectKey());
        }
        return String.join("", document.networkJsonChunks());
    }

    private DynamicTableDefinition table(
            String tableId,
            String label,
            List<DynamicTableColumn> columns,
            List<Map<String, Object>> sourceRows,
            int page,
            int size,
            String defaultSort) {
        int from = Math.min(page * size, sourceRows.size());
        int to = Math.min(from + size, sourceRows.size());
        List<DynamicTableRow> rows = new ArrayList<>();
        for (int index = from; index < to; index++) {
            Map<String, Object> row = sourceRows.get(index);
            rows.add(new DynamicTableRow(String.valueOf(row.getOrDefault("id", tableId + "-" + index)), row));
        }
        return new DynamicTableDefinition(tableId, label, columns, rows, sourceRows.size(), defaultSort);
    }

    private boolean matches(Map<String, Object> row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.toLowerCase(Locale.ROOT);
        return row.values().stream().anyMatch(value -> String.valueOf(value).toLowerCase(Locale.ROOT).contains(query));
    }

    private Map<String, Object> substationRow(Substation substation) {
        return identifiableRow(substation,
                "country", substation.getCountry().map(Enum::name).orElse(""),
                "tso", substation.getTso());
    }

    private Map<String, Object> voltageLevelRow(VoltageLevel voltageLevel) {
        return identifiableRow(voltageLevel,
                "substationId", voltageLevel.getSubstation().map(Identifiable::getId).orElse(""),
                "nominalV", voltageLevel.getNominalV(),
                "lowVoltageLimit", voltageLevel.getLowVoltageLimit(),
                "highVoltageLimit", voltageLevel.getHighVoltageLimit(),
                "topologyKind", voltageLevel.getTopologyKind().name());
    }

    private Map<String, Object> busRow(Bus bus) {
        return identifiableRow(bus,
                "voltageLevelId", bus.getVoltageLevel().getId(),
                "v", bus.getV(),
                "angle", bus.getAngle(),
                "p", bus.getP(),
                "q", bus.getQ(),
                "connectedTerminalCount", bus.getConnectedTerminalCount());
    }

    private Map<String, Object> lineRow(Line line) {
        return identifiableRow(line,
                "voltageLevel1", voltageLevelId(line.getTerminal1()),
                "voltageLevel2", voltageLevelId(line.getTerminal2()),
                "r", line.getR(),
                "x", line.getX(),
                "g1", line.getG1(),
                "g2", line.getG2(),
                "b1", line.getB1(),
                "b2", line.getB2());
    }

    private Map<String, Object> generatorRow(Generator generator) {
        return identifiableRow(generator,
                "voltageLevelId", voltageLevelId(generator.getTerminal()),
                "energySource", generator.getEnergySource().name(),
                "minP", generator.getMinP(),
                "maxP", generator.getMaxP(),
                "targetP", generator.getTargetP(),
                "targetQ", generator.getTargetQ(),
                "targetV", generator.getTargetV(),
                "voltageRegulatorOn", generator.isVoltageRegulatorOn());
    }

    private Map<String, Object> loadRow(Load load) {
        return identifiableRow(load,
                "voltageLevelId", voltageLevelId(load.getTerminal()),
                "loadType", load.getLoadType().name(),
                "p0", load.getP0(),
                "q0", load.getQ0());
    }

    private Map<String, Object> switchRow(Switch networkSwitch) {
        return identifiableRow(networkSwitch,
                "voltageLevelId", networkSwitch.getVoltageLevel().getId(),
                "kind", networkSwitch.getKind().name(),
                "open", networkSwitch.isOpen(),
                "retained", networkSwitch.isRetained());
    }

    private Map<String, Object> busbarSectionRow(BusbarSection busbarSection) {
        return identifiableRow(busbarSection,
                "voltageLevelId", voltageLevelId(busbarSection.getTerminal()),
                "v", busbarSection.getV(),
                "angle", busbarSection.getAngle());
    }

    private Map<String, Object> identifiableRow(Identifiable<?> identifiable, Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", identifiable.getId());
        row.put("name", identifiable.getOptionalName().orElse(""));
        row.put("type", identifiable.getType().name());
        for (int index = 0; index < values.length; index += 2) {
            row.put(String.valueOf(values[index]), values[index + 1]);
        }
        return row;
    }

    private String voltageLevelId(Terminal terminal) {
        return terminal == null || terminal.getVoltageLevel() == null ? "" : terminal.getVoltageLevel().getId();
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            row.put(String.valueOf(values[index]), displayValue(values[index + 1]));
        }
        return row;
    }

    private Object displayValue(Object value) {
        if (value instanceof Double number && !Double.isFinite(number)) {
            return "";
        }
        if (value instanceof Float number && !Float.isFinite(number)) {
            return "";
        }
        return value;
    }

    private Map<String, Object> row(String field, Object value) {
        return map("field", field, "value", value);
    }

    private List<DynamicTableColumn> substationColumns() {
        return columns("id", "ID", "name", "Name", "country", "Country", "tso", "TSO");
    }

    private List<DynamicTableColumn> voltageLevelColumns() {
        return columns("id", "ID", "name", "Name", "substationId", "Substation", "nominalV", "Nominal V",
                "lowVoltageLimit", "Low V", "highVoltageLimit", "High V", "topologyKind", "Topology");
    }

    private List<DynamicTableColumn> busColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "v", "V",
                "angle", "Angle", "p", "P", "q", "Q", "connectedTerminalCount", "Terminals");
    }

    private List<DynamicTableColumn> lineColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevel1", "Voltage level 1", "voltageLevel2", "Voltage level 2",
                "r", "R", "x", "X", "g1", "G1", "g2", "G2", "b1", "B1", "b2", "B2");
    }

    private List<DynamicTableColumn> generatorColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "energySource", "Energy source",
                "minP", "Min P", "maxP", "Max P", "targetP", "Target P", "targetQ", "Target Q",
                "targetV", "Target V", "voltageRegulatorOn", "Regulator");
    }

    private List<DynamicTableColumn> loadColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "loadType", "Load type",
                "p0", "P0", "q0", "Q0");
    }

    private List<DynamicTableColumn> switchColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "kind", "Kind",
                "open", "Open", "retained", "Retained");
    }

    private List<DynamicTableColumn> busbarSectionColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "v", "V", "angle", "Angle");
    }

    private List<DynamicTableColumn> positionColumns() {
        return columns("id", "ID", "name", "Name", "elementType", "Element type", "extensionType", "Extension",
                "canonicalId", "Canonical ID", "sequenceNumber", "Sequence", "latitude", "Latitude", "longitude", "Longitude",
                "xPosition", "X", "yPosition", "Y", "zPosition", "Z");
    }

    private String canonicalId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        int hash = normalized.lastIndexOf('#');
        int slash = normalized.lastIndexOf('/');
        int index = Math.max(hash, slash);
        if (index >= 0 && index < normalized.length() - 1) {
            normalized = normalized.substring(index + 1);
        }
        while (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private List<DynamicTableColumn> columns(String... keyLabels) {
        List<DynamicTableColumn> columns = new ArrayList<>();
        for (int index = 0; index < keyLabels.length; index += 2) {
            columns.add(new DynamicTableColumn(keyLabels[index], keyLabels[index + 1], "string", true, true, ""));
        }
        return columns;
    }

    private <T> List<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).toList();
    }

    private record IidmTableSpec(
            String tableId,
            String label,
            List<DynamicTableColumn> columns,
            List<Map<String, Object>> rows,
            String defaultSort,
            int totalRows) {
        IidmTableSpec(String tableId, String label, List<DynamicTableColumn> columns, List<Map<String, Object>> rows, String defaultSort) {
            this(tableId, label, columns, rows, defaultSort, rows.size());
        }

        IidmTableSpec withoutRows() {
            return new IidmTableSpec(tableId, label, columns, List.of(), defaultSort, totalRows);
        }
    }

    private record GridPoint(String rowId, String locationId, int sequenceNumber, double latitude, double longitude, String sourceFileId) {
        boolean valid() {
            return Double.isFinite(latitude) && Double.isFinite(longitude);
        }

        String label() {
            return rowId + " (" + latitude + ", " + longitude + ")";
        }
    }

    private record GridViewSvg(String svg, int coordinateCount, int lineCount, int substationCount, List<String> diagnostics) {
        GridViewSvg {
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }
}
