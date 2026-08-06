package eu.egm.srv.common.lfsa.service.sensitivity;

import com.infra.InfrastructureUtils;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSort;
import com.infra.storage.object.ObjectStorageService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityOperatorStrategiesCalculationMode;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.SensitivityVariableType;
import com.utils.restservice.RestServiceSupport;
import eu.egm.data.cnm.common.IidmTransformationStatus;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.common.lfsa.common.CommonPage;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisConfiguration;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisConfigurationSaveRequest;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisParametersDto;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRequested;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunDetail;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunStartRequest;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunState;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunSummary;
import eu.egm.data.common.lfsa.sensitivity.SensitivityFactorDto;
import eu.egm.data.common.lfsa.sensitivity.SensitivityInputTable;
import eu.egm.data.common.lfsa.sensitivity.SensitivityInputUploadResponse;
import eu.egm.data.common.lfsa.sensitivity.SensitivityIidmNetworkSummary;
import eu.egm.data.common.lfsa.sensitivity.SensitivityMatrixRow;
import eu.egm.data.iidm.network.IidmNetworkXiidm;
import eu.egm.srv.common.lfsa.config.sensitivity.SensitivityDefaults;
import eu.egm.srv.common.lfsa.config.sensitivity.SensitivityDefaultsService;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocument;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocumentAdapter;
import eu.egm.srv.common.lfsa.domain.IidmNetworkReadDocument;
import eu.egm.srv.common.lfsa.domain.IidmNetworkReadDocumentAdapter;
import eu.egm.srv.common.lfsa.domain.sensitivity.SensitivityAnalysisConfigurationDocument;
import eu.egm.srv.common.lfsa.domain.sensitivity.SensitivityAnalysisConfigurationDocumentAdapter;
import eu.egm.srv.common.lfsa.domain.sensitivity.SensitivityAnalysisRunDocument;
import eu.egm.srv.common.lfsa.domain.sensitivity.SensitivityAnalysisRunDocumentAdapter;
import io.micrometer.observation.ObservationRegistry;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Coordinates asynchronous PowSyBl sensitivity-analysis runs from persisted IIDM network documents.
 */
@Service
public class SensitivityAnalysisService extends RestServiceSupport {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final InfrastructureUtils infrastructureUtils;
    private final SensitivityDefaultsService defaultsService;
    private final DocumentRepositoryService<CnmImportReadDocument> importRepository;
    private final DocumentRepositoryService<IidmNetworkReadDocument> iidmNetworkRepository;
    private final DocumentRepositoryService<SensitivityAnalysisConfigurationDocument> configurationRepository;
    private final DocumentRepositoryService<SensitivityAnalysisRunDocument> runRepository;
    private final ObjectStorageService objectStorageService;
    private final String inputBucket;
    private final String exchange;
    private final String routingKey;

    public SensitivityAnalysisService(
            Environment environment,
            ObservationRegistry observationRegistry,
            InfrastructureUtils infrastructureUtils,
            SensitivityDefaultsService defaultsService,
            @Value("${lfsa.sensitivity.event.exchange:lfsa.events}") String exchange,
            @Value("${lfsa.sensitivity.event.requested-routing-key:lfsa.sensitivity.requested}") String routingKey,
            @Value("${lfsa.sensitivity.inputs.bucket:lfsa-inputs}") String inputBucket) {
        super(environment, observationRegistry);
        this.infrastructureUtils = infrastructureUtils;
        this.defaultsService = defaultsService;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.inputBucket = inputBucket;
        this.importRepository = infrastructureUtils.documentRepository(new CnmImportReadDocumentAdapter());
        this.iidmNetworkRepository = infrastructureUtils.documentRepository(new IidmNetworkReadDocumentAdapter());
        this.configurationRepository =
                infrastructureUtils.documentRepository(new SensitivityAnalysisConfigurationDocumentAdapter());
        this.runRepository = infrastructureUtils.documentRepository(new SensitivityAnalysisRunDocumentAdapter());
        this.objectStorageService = infrastructureUtils.objectStorageService();
    }

    public CommonPage<SensitivityIidmNetworkSummary> completedIidmNetworks(String importId, int page, int size) {
        requireIidmReadyImport(importId);
        List<SensitivityIidmNetworkSummary> rows = iidmNetworkRepository.findByField("importId", importId, 1000)
                .stream()
                .sorted(Comparator.comparing(IidmNetworkReadDocument::id))
                .map(this::toNetworkSummary)
                .toList();
        return page(rows, page, size);
    }

    public SensitivityAnalysisConfiguration defaultConfiguration() {
        return new SensitivityAnalysisConfiguration(
                "",
                "Default Sensitivity",
                "DEFAULT",
                "",
                "",
                defaultsService.load().parameters());
    }

    public CommonPage<SensitivityAnalysisConfiguration> configurations(int page, int size) {
        List<SensitivityAnalysisConfiguration> rows = configurationRepository
                .findAll(defaultsService.load().maxSearchRuns(), DocumentSort.descending("updatedAt"))
                .stream()
                .map(this::toConfiguration)
                .toList();
        return page(rows, page, size);
    }

    public SensitivityAnalysisConfiguration saveConfiguration(SensitivityAnalysisConfigurationSaveRequest request) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        String name = request.name() == null || request.name().isBlank()
                ? DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(now)
                        + "_SENS_Conf"
                : request.name().trim();
        SensitivityAnalysisConfigurationDocument document = new SensitivityAnalysisConfigurationDocument(
                id,
                name,
                "USER",
                now,
                now,
                request.parameters() == null ? defaultsService.load().parameters() : request.parameters());
        configurationRepository.save(document);
        return toConfiguration(document);
    }

    public SensitivityInputUploadResponse uploadInput(String kind, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Sensitivity input file is required");
        }
        String normalizedKind = normalizeInputKind(kind);
        String fileName = safeFileName(file.getOriginalFilename());
        String objectId = "sensitivity-inputs/%s/%s-%s"
                .formatted(normalizedKind.toLowerCase(), UUID.randomUUID(), fileName);
        try {
            objectStorageService.store(
                    inputBucket,
                    objectId,
                    file.getBytes(),
                    file.getContentType() == null || file.getContentType().isBlank()
                            ? "application/octet-stream"
                            : file.getContentType());
            return new SensitivityInputUploadResponse(normalizedKind, fileName, objectId, file.getSize());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to store sensitivity input " + fileName, exception);
        }
    }

    public SensitivityAnalysisRunSummary startRun(SensitivityAnalysisRunStartRequest request) {
        String importId = requireValue(request.fileImportId(), "fileImportId");
        requireIidmReadyImport(importId);
        SensitivityAnalysisConfiguration configuration = resolveConfiguration(request.configurationId());
        List<IidmNetworkReadDocument> networks = request.iidmNetworkIds().isEmpty()
                ? iidmNetworkRepository.findByField("importId", importId, 1000)
                : request.iidmNetworkIds().stream()
                        .flatMap(id -> iidmNetworkRepository.findByField("id", id, 1).stream())
                        .toList();
        Instant now = Instant.now();
        String runId = UUID.randomUUID().toString();
        Map<String, String> inputReferences = inputReferences(request);
        SensitivityAnalysisRunDocument document = new SensitivityAnalysisRunDocument(
                runId,
                importId,
                SensitivityAnalysisRunState.STARTED,
                now,
                null,
                null,
                configuration.id(),
                configuration.name(),
                configuration.parameters(),
                networks.stream().map(IidmNetworkReadDocument::id).toList(),
                inputReferences,
                List.of(),
                List.of(),
                Map.of(),
                List.of("Sensitivity analysis queued for " + networks.size() + " IIDM network document(s)"),
                "Sensitivity analysis started");
        runRepository.save(document);
        infrastructureUtils.eventPublisher().publish(
                exchange,
                routingKey,
                new SensitivityAnalysisRequested(runId, importId, document.iidmNetworkIds(), now.toString()));
        return toSummary(document);
    }

    public void process(SensitivityAnalysisRequested event) {
        SensitivityAnalysisRunDocument current = findRun(event.runId())
                .orElseThrow(() -> new IllegalArgumentException("Sensitivity run not found: " + event.runId()));
        List<String> diagnostics = new ArrayList<>(current.diagnostics());
        try {
            List<IidmNetworkReadDocument> documents = event.iidmNetworkIds().isEmpty()
                    ? iidmNetworkRepository.findByField("importId", event.fileImportId(), 1000)
                    : event.iidmNetworkIds().stream()
                            .flatMap(id -> iidmNetworkRepository.findByField("id", id, 1).stream())
                            .toList();
            if (documents.isEmpty()) {
                throw new IllegalStateException("No IIDM networks found for sensitivity run " + event.runId());
            }
            List<Network> networks = documents.stream().map(this::readNetwork).filter(Objects::nonNull).toList();
            Network network = mergeInMemory(networks, diagnostics);
            Map<String, Long> counts = elementCounts(network);
            SensitivityAnalysisParametersDto parameters = current.parameters();
            List<SensitivityFactor> factors = factors(network, parameters, diagnostics);
            List<Contingency> contingencies = contingencies(network, parameters, diagnostics);
            String workingVariantId = workingVariantId(network);
            diagnostics.add("Using IIDM working variant " + workingVariantId + " for sensitivity analysis");
            SensitivityAnalysisResult result;
            try (LocalComputationManager computationManager = new LocalComputationManager()) {
                result = SensitivityAnalysis.run(
                        network,
                        workingVariantId,
                        factors,
                        contingencies,
                        List.of(),
                        toPowSyBlParameters(parameters),
                        computationManager,
                        ReportNode.NO_OP);
            }
            List<SensitivityFactorDto> factorRows = factors.stream().map(this::toFactorDto).toList();
            List<SensitivityMatrixRow> matrixRows = matrixRows(result, factors);
            diagnostics.add("Sensitivity analysis produced " + matrixRows.size() + " result row(s)");
            runRepository.save(new SensitivityAnalysisRunDocument(
                    current.id(),
                    current.fileImportId(),
                    SensitivityAnalysisRunState.DONE,
                    current.startedAt(),
                    Instant.now(),
                    null,
                    current.configurationId(),
                    current.configurationName(),
                    current.parameters(),
                    documents.stream().map(IidmNetworkReadDocument::id).toList(),
                    current.inputReferences(),
                    factorRows,
                    matrixRows.stream().limit(defaultsService.load().maxResultRows()).toList(),
                    counts,
                    bounded(diagnostics),
                    "Sensitivity analysis completed"));
        } catch (Exception exception) {
            diagnostics.add(rootMessage(exception));
            logger.error("{} failed to process sensitivity-analysis run {}", moduleName(), event.runId(), exception);
            runRepository.save(new SensitivityAnalysisRunDocument(
                    current.id(),
                    current.fileImportId(),
                    SensitivityAnalysisRunState.FAILED,
                    current.startedAt(),
                    null,
                    Instant.now(),
                    current.configurationId(),
                    current.configurationName(),
                    current.parameters(),
                    current.iidmNetworkIds(),
                    current.inputReferences(),
                    current.factors(),
                    current.matrixRows(),
                    current.networkElementCounts(),
                    bounded(diagnostics),
                    exception.getMessage()));
        }
    }

    public CommonPage<SensitivityAnalysisRunSummary> searchRuns(String runId, String runDate, String runTime, int page, int size) {
        List<SensitivityAnalysisRunSummary> rows = runRepository
                .findAll(defaultsService.load().maxSearchRuns(), DocumentSort.descending("startedAt"))
                .stream()
                .map(this::toSummary)
                .filter(summary -> matches(runId, summary.runId()))
                .filter(summary -> matches(runDate, summary.runDate()))
                .filter(summary -> matches(runTime, summary.runTime()))
                .toList();
        return page(rows, page, size);
    }

    public SensitivityAnalysisRunDetail detail(String runId) {
        SensitivityAnalysisRunDocument document = findRun(runId)
                .orElseThrow(() -> new IllegalArgumentException("Sensitivity run not found: " + runId));
        return new SensitivityAnalysisRunDetail(
                toSummary(document),
                document.configuration(),
                document.iidmNetworkIds(),
                document.inputReferences(),
                document.factors(),
                document.matrixRows(),
                document.networkElementCounts(),
                document.diagnostics());
    }

    public SensitivityInputTable inputTable(String runId, String kind) {
        SensitivityAnalysisRunDocument document = findRun(runId)
                .orElseThrow(() -> new IllegalArgumentException("Sensitivity run not found: " + runId));
        String normalizedKind = normalizeInputKind(kind);
        String objectId = document.inputReferences().get(normalizedKind.toLowerCase() + "ObjectId");
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException(normalizedKind + " input is not available for run " + runId);
        }
        byte[] bytes = objectStorageService.read(inputBucket, objectId);
        return new SensitivityInputTable(normalizedKind, objectId, parseInputRows(bytes));
    }

    private Optional<SensitivityAnalysisRunDocument> findRun(String runId) {
        return runRepository.findByField("id", runId, 1).stream().findFirst();
    }

    private Network readNetwork(IidmNetworkReadDocument document) {
        String xiidm = document.networkXiidm().isBlank()
                ? String.join("", document.networkXiidmChunks())
                : document.networkXiidm();
        return xiidm.isBlank() ? null : IidmNetworkXiidm.read(xiidm);
    }

    private SensitivityIidmNetworkSummary toNetworkSummary(IidmNetworkReadDocument document) {
        return new SensitivityIidmNetworkSummary(
                document.id(),
                document.importId(),
                document.sourceFileIds(),
                document.sourceFileNames(),
                document.businessDay(),
                document.businessTime(),
                document.timeFrame(),
                document.tsoName(),
                document.networkFormat());
    }

    private void requireIidmReadyImport(String importId) {
        CnmImportReadDocument document = importRepository.findByField("id", importId, 1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Import not found: " + importId));
        if (!isIidmReadyImport(document)) {
            throw new IllegalStateException("Import " + importId
                    + " is not ready for sensitivity analysis. IIDM status is "
                    + document.iidmTransformationStatus());
        }
    }

    private boolean isIidmReadyImport(CnmImportReadDocument document) {
        return document.iidmTransformationStatus() == IidmTransformationStatus.DONE
                && document.state() == ImportState.SUCCESS;
    }

    private Network mergeInMemory(List<Network> networks, List<String> diagnostics) {
        if (networks.size() == 1) {
            diagnostics.add("One IIDM network found; merge step skipped");
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
            diagnostics.add("PowSyBl NetworkMerger completed in memory");
            return merged;
        } catch (ReflectiveOperationException exception) {
            diagnostics.add("PowSyBl NetworkMerger is not available; first network used");
            return networks.getFirst();
        }
    }

    private String workingVariantId(Network network) {
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        if (workingVariantId == null || workingVariantId.isBlank()) {
            throw new IllegalStateException("IIDM network has no active working variant for sensitivity analysis");
        }
        return workingVariantId;
    }

    private List<SensitivityFactor> factors(
            Network network,
            SensitivityAnalysisParametersDto parameters,
            List<String> diagnostics) {
        SensitivityFunctionType functionType = enumValue(
                SensitivityFunctionType.class,
                parameters.functionType(),
                SensitivityFunctionType.BRANCH_ACTIVE_POWER_1);
        SensitivityVariableType variableType = enumValue(
                SensitivityVariableType.class,
                parameters.variableType(),
                SensitivityVariableType.INJECTION_ACTIVE_POWER);
        ContingencyContext context = contingencyContext(parameters.contingencyContext());
        List<String> functionIds = network.getLineStream()
                .sorted(Comparator.comparing(Line::getId))
                .limit(parameters.maxMonitoredBranches())
                .map(Line::getId)
                .toList();
        List<String> variableIds = network.getGeneratorStream()
                .sorted(Comparator.comparing(Generator::getId))
                .limit(parameters.maxVariables())
                .map(Generator::getId)
                .toList();
        if (functionIds.isEmpty() || variableIds.isEmpty()) {
            diagnostics.add("No lines or generators available to create sensitivity factors");
        }
        return SensitivityFactor.createMatrix(functionType, functionIds, variableType, variableIds, false, context);
    }

    private List<Contingency> contingencies(
            Network network,
            SensitivityAnalysisParametersDto parameters,
            List<String> diagnostics) {
        if ("NONE".equalsIgnoreCase(parameters.contingencyContext())) {
            return List.of();
        }
        List<Contingency> contingencies = network.getLineStream()
                .sorted(Comparator.comparing(Line::getId))
                .limit(parameters.maxGeneratedContingencies())
                .map(line -> Contingency.line(line.getId(), "N-1-" + line.getId()))
                .filter(contingency -> contingency.isValid(network))
                .toList();
        diagnostics.add("Generated " + contingencies.size() + " sensitivity contingency context(s)");
        return contingencies;
    }

    private SensitivityAnalysisParameters toPowSyBlParameters(SensitivityAnalysisParametersDto dto) {
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(new LoadFlowParameters().setDc(dto.dc()));
        parameters.setFlowFlowSensitivityValueThreshold(dto.flowFlowSensitivityValueThreshold());
        parameters.setVoltageVoltageSensitivityValueThreshold(dto.voltageVoltageSensitivityValueThreshold());
        parameters.setFlowVoltageSensitivityValueThreshold(dto.flowVoltageSensitivityValueThreshold());
        parameters.setAngleFlowSensitivityValueThreshold(dto.angleFlowSensitivityValueThreshold());
        parameters.setOperatorStrategiesCalculationMode(enumValue(
                SensitivityOperatorStrategiesCalculationMode.class,
                dto.operatorStrategiesCalculationMode(),
                SensitivityOperatorStrategiesCalculationMode.NONE));
        if (!dto.debugDir().isBlank()) {
            parameters.setDebugDir(dto.debugDir());
        }
        return parameters;
    }

    private List<SensitivityMatrixRow> matrixRows(SensitivityAnalysisResult result, List<SensitivityFactor> factors) {
        List<String> contingencies = result.getContingencyIds();
        return result.getValues().stream()
                .map(value -> toMatrixRow(value, factors, contingencies))
                .toList();
    }

    private SensitivityMatrixRow toMatrixRow(
            SensitivityValue value,
            List<SensitivityFactor> factors,
            List<String> contingencies) {
        SensitivityFactor factor = factors.get(value.getFactorIndex());
        String contingencyId = value.getContingencyIndex() < 0 || value.getContingencyIndex() >= contingencies.size()
                ? "BASE"
                : contingencies.get(value.getContingencyIndex());
        return new SensitivityMatrixRow(
                factor.getFunctionType().name(),
                factor.getFunctionId(),
                factor.getVariableType().name(),
                factor.getVariableId(),
                contingencyId,
                finite(value.getValue()),
                finite(value.getFunctionReference()));
    }

    private SensitivityFactorDto toFactorDto(SensitivityFactor factor) {
        return new SensitivityFactorDto(
                factor.getFunctionType().name(),
                factor.getFunctionId(),
                factor.getVariableType().name(),
                factor.getVariableId(),
                String.valueOf(factor.getContingencyContext().getContextType()));
    }

    private Map<String, Long> elementCounts(Network network) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("substations", network.getSubstationStream().count());
        counts.put("voltageLevels", network.getVoltageLevelStream().count());
        counts.put("buses", network.getBusView().getBusStream().count());
        counts.put("lines", network.getLineStream().count());
        counts.put("generators", network.getGeneratorStream().count());
        counts.put("loads", network.getLoadStream().count());
        counts.put("switches", network.getSwitchStream().count());
        return counts;
    }

    private SensitivityAnalysisConfiguration resolveConfiguration(String id) {
        if (id == null || id.isBlank()) {
            return defaultConfiguration();
        }
        return configurationRepository.findByField("id", id, 1).stream()
                .findFirst()
                .map(this::toConfiguration)
                .orElse(defaultConfiguration());
    }

    private SensitivityAnalysisConfiguration toConfiguration(SensitivityAnalysisConfigurationDocument document) {
        return new SensitivityAnalysisConfiguration(
                document.id(),
                document.name(),
                document.source(),
                instantString(document.createdAt()),
                instantString(document.updatedAt()),
                document.parameters() == null ? defaultsService.load().parameters() : document.parameters());
    }

    private SensitivityAnalysisRunSummary toSummary(SensitivityAnalysisRunDocument document) {
        Instant startedAt = instant(document.startedAt()).orElse(Instant.now());
        Map<String, String> inputReferences = document.inputReferences();
        return new SensitivityAnalysisRunSummary(
                document.id(),
                document.fileImportId(),
                document.state(),
                DATE.format(startedAt.atZone(ZoneOffset.UTC)),
                TIME.format(startedAt.atZone(ZoneOffset.UTC)),
                document.iidmNetworkIds().size(),
                document.factors().size(),
                document.matrixRows().size(),
                document.diagnostics().size(),
                inputReferences.getOrDefault("ptdfObjectId", ""),
                inputReferences.getOrDefault("lodfObjectId", ""),
                inputReferences.getOrDefault("glskObjectId", ""),
                document.message());
    }

    private Map<String, String> inputReferences(SensitivityAnalysisRunStartRequest request) {
        SensitivityDefaults defaults = defaultsService.load();
        Map<String, String> references = new LinkedHashMap<>();
        references.put("ptdfObjectId", defaultValue(request.ptdfObjectId(), defaults.defaultPtdfObjectId()));
        references.put("lodfObjectId", defaultValue(request.lodfObjectId(), defaults.defaultLodfObjectId()));
        references.put("glskObjectId", defaultValue(request.glskObjectId(), defaults.defaultGlskObjectId()));
        return references;
    }

    private List<String> bounded(List<String> diagnostics) {
        return diagnostics.stream().limit(defaultsService.load().maxDiagnostics()).toList();
    }

    private ContingencyContext contingencyContext(String value) {
        if ("NONE".equalsIgnoreCase(value)) {
            return ContingencyContext.none();
        }
        if ("ONLY_CONTINGENCIES".equalsIgnoreCase(value)) {
            return ContingencyContext.onlyContingencies();
        }
        return ContingencyContext.all();
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private <T> CommonPage<T> page(List<T> rows, int page, int size) {
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int from = Math.min(rows.size(), safePage * safeSize);
        int to = Math.min(rows.size(), from + safeSize);
        return new CommonPage<>(rows.subList(from, to), rows.size(), safePage, safeSize);
    }

    private boolean matches(String filter, String value) {
        return filter == null || filter.isBlank() || value != null && value.contains(filter);
    }

    private Optional<Instant> instant(Object value) {
        if (value instanceof Instant instant) {
            return Optional.of(instant);
        }
        if (value instanceof Number number) {
            return Optional.of(Instant.ofEpochMilli(number.longValue()));
        }
        try {
            return value == null ? Optional.empty() : Optional.of(Instant.parse(String.valueOf(value)));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String instantString(Object value) {
        return instant(value).map(Instant::toString).orElse("");
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeInputKind(String kind) {
        String normalized = requireValue(kind, "Sensitivity input kind").trim().toUpperCase();
        if (!List.of("PTDF", "LODF", "GLSK").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported sensitivity input kind " + kind);
        }
        return normalized;
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "input.dat";
        }
        String normalized = fileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private List<Map<String, Object>> parseInputRows(byte[] bytes) {
        String content = new String(bytes, StandardCharsets.UTF_8);
        List<String> lines = content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(defaultsService.load().maxResultRows())
                .toList();
        if (lines.isEmpty()) {
            return List.of();
        }
        String delimiter = delimiter(lines.getFirst());
        if (delimiter.isBlank()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int index = 0; index < lines.size(); index++) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("line", index + 1);
                row.put("value", lines.get(index));
                rows.add(row);
            }
            return rows;
        }
        String[] headers = split(lines.getFirst(), delimiter);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String[] values = split(lines.get(index), delimiter);
            Map<String, Object> row = new LinkedHashMap<>();
            for (int column = 0; column < Math.max(headers.length, values.length); column++) {
                String header = column < headers.length && !headers[column].isBlank()
                        ? headers[column]
                        : "column" + (column + 1);
                row.put(header, column < values.length ? values[column] : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private String delimiter(String header) {
        if (header.contains("\t")) {
            return "\t";
        }
        if (header.contains(";")) {
            return ";";
        }
        if (header.contains(",")) {
            return ",";
        }
        return "";
    }

    private String[] split(String line, String delimiter) {
        return line.split(Pattern.quote(delimiter), -1);
    }

    private String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getClass().getSimpleName() + ": " + cursor.getMessage();
    }

    private double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

}
