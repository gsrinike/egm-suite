package eu.egm.srv.common.lfsa.service;

import com.infra.InfrastructureUtils;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSort;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.violations.LimitViolation;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowRunParameters;
import com.powsybl.security.SecurityAnalysis;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisReport;
import com.powsybl.security.SecurityAnalysisRunParameters;
import com.utils.restservice.RestServiceSupport;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.IidmTransformationStatus;
import eu.egm.data.common.lfsa.common.AnalysisStepState;
import eu.egm.data.common.lfsa.common.CommonPage;
import eu.egm.data.common.lfsa.common.ContingencyViolation;
import eu.egm.data.common.lfsa.common.LfSaParameterConfiguration;
import eu.egm.data.common.lfsa.common.LfSaParameterConfigurationSaveRequest;
import eu.egm.data.common.lfsa.common.LineFlow;
import eu.egm.data.common.lfsa.common.LoadFlowComputationResult;
import eu.egm.data.common.lfsa.common.LoadFlowParametersDto;
import eu.egm.data.common.lfsa.common.LoadFlowRequest;
import eu.egm.data.common.lfsa.common.LoadFlowResult;
import eu.egm.data.common.lfsa.common.LoadFlowStrategy;
import eu.egm.data.common.lfsa.common.SecurityAnalysisImportCandidate;
import eu.egm.data.common.lfsa.common.SecurityAnalysisComputationResult;
import eu.egm.data.common.lfsa.common.SecurityAnalysisParametersDto;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRequest;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRequested;
import eu.egm.data.common.lfsa.common.SecurityAnalysisResult;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunDetail;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunStartRequest;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunState;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunSummary;
import eu.egm.data.common.lfsa.common.ViolationType;
import eu.egm.data.common.lfsa.common.WorkflowStatus;
import eu.egm.data.iidm.network.IidmNetworkXiidm;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocument;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocumentAdapter;
import eu.egm.srv.common.lfsa.domain.IidmNetworkReadDocument;
import eu.egm.srv.common.lfsa.domain.IidmNetworkReadDocumentAdapter;
import eu.egm.srv.common.lfsa.config.LfSaDefaults;
import eu.egm.srv.common.lfsa.config.LfSaDefaultsService;
import eu.egm.srv.common.lfsa.domain.SecurityAnalysisParameterConfigurationDocument;
import eu.egm.srv.common.lfsa.domain.SecurityAnalysisParameterConfigurationDocumentAdapter;
import eu.egm.srv.common.lfsa.domain.SecurityAnalysisRunDocument;
import eu.egm.srv.common.lfsa.domain.SecurityAnalysisRunDocumentAdapter;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class LfSaService extends RestServiceSupport {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final InfrastructureUtils infrastructureUtils;
    private final LfSaDefaultsService defaultsService;
    private final DocumentRepositoryService<CnmImportReadDocument> importRepository;
    private final DocumentRepositoryService<IidmNetworkReadDocument> iidmNetworkRepository;
    private final DocumentRepositoryService<SecurityAnalysisRunDocument> runRepository;
    private final DocumentRepositoryService<SecurityAnalysisParameterConfigurationDocument> parameterRepository;
    private final String exchange;
    private final String routingKey;

    public LfSaService(
            Environment environment,
            ObservationRegistry observationRegistry,
            InfrastructureUtils infrastructureUtils,
            LfSaDefaultsService defaultsService,
            @Value("${lfsa.security-analysis.event.exchange:lfsa.events}") String exchange,
            @Value("${lfsa.security-analysis.event.requested-routing-key:lfsa.security-analysis.requested}") String routingKey) {
        super(environment, observationRegistry);
        this.infrastructureUtils = infrastructureUtils;
        this.defaultsService = defaultsService;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.importRepository = infrastructureUtils.documentRepository(new CnmImportReadDocumentAdapter());
        this.iidmNetworkRepository = infrastructureUtils.documentRepository(new IidmNetworkReadDocumentAdapter());
        this.runRepository = infrastructureUtils.documentRepository(new SecurityAnalysisRunDocumentAdapter());
        this.parameterRepository = infrastructureUtils.documentRepository(new SecurityAnalysisParameterConfigurationDocumentAdapter());
    }

    public LoadFlowResult runLoadFlow(LoadFlowRequest request) {
        logger.info("{} running compatibility load flow for {}", moduleName(), request.networkCase());
        List<LineFlow> flows = List.of(
                new LineFlow("LINE-1", "BUS-1", "BUS-2", 184.0, 32.0, 74.5),
                new LineFlow("LINE-2", "BUS-2", "BUS-4", 226.0, 41.0, 96.2),
                new LineFlow("LINE-3", "BUS-3", "BUS-5", 248.0, 39.0, 108.4));
        return new LoadFlowResult(
                id(request.requestId(), "lf"),
                WorkflowStatus.COMPLETED,
                flows,
                Instant.now(),
                "Load flow completed with deterministic baseline results");
    }

    public SecurityAnalysisResult runSecurityAnalysis(SecurityAnalysisRequest request) {
        logger.info("{} running compatibility security analysis for {}", moduleName(), request.networkCase());
        List<ContingencyViolation> pre = List.of(
                new ContingencyViolation("BASE", "LINE-3", ViolationType.OVERLOAD, 108.4, 100.0, "%", "MEDIUM"));
        List<ContingencyViolation> post = List.of(
                new ContingencyViolation(contingency(request, 0), "LINE-7", ViolationType.OVERLOAD, 126.8, 100.0, "%", "HIGH"),
                new ContingencyViolation(contingency(request, 1), "BUS-12", ViolationType.VOLTAGE_LOW, 0.91, 0.95, "pu", "MEDIUM"));
        return new SecurityAnalysisResult(
                id(request.requestId(), "sa"),
                WorkflowStatus.COMPLETED,
                pre,
                post,
                Instant.now(),
                "Security analysis completed with deterministic violation set");
    }

    public CommonPage<SecurityAnalysisImportCandidate> searchSuccessfulImports(
            String service,
            String timeFrame,
            String date,
            int page,
            int size) {
        LfSaDefaults defaults = defaultsService.load();
        List<SecurityAnalysisImportCandidate> rows = importRepository
                .findAll(defaults.maxSearchImports(), DocumentSort.descending("createdAt"))
                .stream()
                .filter(this::isAnalysisReadyImport)
                .filter(importDocument -> matches(service, value(importDocument.serviceType())))
                .filter(importDocument -> matches(timeFrame, value(importDocument.timeFrame())))
                .map(this::toCandidate)
                .filter(candidate -> matchesDate(date, candidate))
                .toList();
        return page(rows, page, size);
    }

    public LfSaParameterConfiguration defaultParameterConfiguration() {
        LfSaDefaults defaults = defaultsService.load();
        return new LfSaParameterConfiguration(
                "",
                "Default LFnSA",
                "DEFAULT",
                "",
                "",
                defaults.loadFlowStrategy(),
                defaults.loadFlowParameters(),
                defaults.securityAnalysisParameters());
    }

    public CommonPage<LfSaParameterConfiguration> parameterConfigurations(int page, int size) {
        List<LfSaParameterConfiguration> rows = parameterRepository
                .findAll(defaultsService.load().maxSearchRuns(), DocumentSort.descending("updatedAt"))
                .stream()
                .map(this::toParameterConfiguration)
                .toList();
        return page(rows, page, size);
    }

    public LfSaParameterConfiguration saveParameterConfiguration(
            LfSaParameterConfigurationSaveRequest request) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        String name = request.name() == null || request.name().isBlank()
                ? DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(now)
                        + "_SA_Conf"
                : request.name().trim();
        LfSaDefaults defaults = defaultsService.load();
        LoadFlowStrategy loadFlowStrategy = request.loadFlowStrategy() == null
                ? defaults.loadFlowStrategy()
                : request.loadFlowStrategy();
        LoadFlowParametersDto loadFlowParameters = request.loadFlowParameters() == null
                ? defaults.loadFlowParameters()
                : request.loadFlowParameters();
        SecurityAnalysisParametersDto securityAnalysisParameters = request.securityAnalysisParameters() == null
                ? defaults.securityAnalysisParameters()
                : request.securityAnalysisParameters();
        SecurityAnalysisParameterConfigurationDocument document = new SecurityAnalysisParameterConfigurationDocument(
                id,
                name,
                "USER",
                now,
                now,
                loadFlowStrategy,
                loadFlowParameters,
                securityAnalysisParameters);
        parameterRepository.save(document);
        return toParameterConfiguration(document);
    }

    public SecurityAnalysisRunSummary startSecurityAnalysis(SecurityAnalysisRunStartRequest request) {
        String importId = requireValue(request.fileImportId(), "fileImportId");
        LfSaParameterConfiguration parameterConfiguration =
                resolveParameterConfiguration(request.parameterConfigurationId());
        List<IidmNetworkReadDocument> networks = iidmNetworkRepository.findByField(
                "importId",
                importId,
                defaultsService.load().maxIidmNetworks());
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        SecurityAnalysisRunDocument document = new SecurityAnalysisRunDocument(
                runId,
                importId,
                SecurityAnalysisRunState.STARTED,
                now,
                null,
                null,
                AnalysisStepState.STARTED,
                AnalysisStepState.NOT_STARTED,
                parameterConfiguration.id(),
                parameterConfiguration.name(),
                parameterConfiguration.loadFlowStrategy(),
                parameterConfiguration.loadFlowParameters(),
                parameterConfiguration.securityAnalysisParameters(),
                null,
                null,
                networks.stream().map(IidmNetworkReadDocument::id).toList(),
                Map.of(),
                List.of(),
                List.of(),
                List.of("Security analysis queued for " + networks.size()
                        + " IIDM network document(s) with parameter set " + parameterConfiguration.name()),
                "Security analysis started");
        runRepository.save(document);
        infrastructureUtils.eventPublisher().publish(
                exchange,
                routingKey,
                new SecurityAnalysisRequested(runId, importId, document.iidmNetworkIds(), now.toString()));
        return toSummary(document);
    }

    public void processSecurityAnalysis(SecurityAnalysisRequested event) {
        SecurityAnalysisRunDocument current = findRun(event.runId())
                .orElseGet(() -> new SecurityAnalysisRunDocument(
                        event.runId(),
                        event.fileImportId(),
                        SecurityAnalysisRunState.STARTED,
                        Instant.now(),
                        null,
                        null,
                        AnalysisStepState.STARTED,
                        AnalysisStepState.NOT_STARTED,
                        "",
                        "Default LFnSA",
                        defaultsService.load().loadFlowStrategy(),
                        defaultsService.load().loadFlowParameters(),
                        defaultsService.load().securityAnalysisParameters(),
                        null,
                        null,
                        event.iidmNetworkIds(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of("Run document was recreated from the event payload"),
                        "Security analysis started"));
        List<String> diagnostics = new ArrayList<>(current.diagnostics());
        AnalysisStepState latestLoadFlowState = current.loadFlowState();
        AnalysisStepState latestSecurityAnalysisState = current.securityAnalysisState();
        LoadFlowComputationResult latestLoadFlowResult = current.loadFlowResult();
        try {
            List<IidmNetworkReadDocument> documents = loadNetworkDocuments(event);
            if (documents.isEmpty()) {
                throw new IllegalStateException("No IIDM network documents found for import " + event.fileImportId());
            }
            List<Network> networks = documents.stream()
                    .map(this::readNetwork)
                    .filter(Objects::nonNull)
                    .toList();
            if (networks.isEmpty()) {
                throw new IllegalStateException("No readable PowSyBl IIDM networks found for import " + event.fileImportId());
            }
            Network merged = mergeInMemory(networks, diagnostics);
            Map<String, Long> counts = elementCounts(merged);
            LoadFlowStrategy selectedStrategy = current.loadFlowStrategy();
            LoadFlowParametersDto selectedLoadFlowParameters = current.loadFlowParameters();
            SecurityAnalysisParametersDto selectedSecurityAnalysisParameters = current.securityAnalysisParameters();
            LoadFlowComputationResult loadFlowResult;
            boolean securityAnalysisDcMode;
            List<LineFlow> lineFlows;
            SecurityAnalysisComputationResult computationResult;
            List<ContingencyViolation> violations;
            List<Contingency> contingencies;
            try (LocalComputationManager computationManager = new LocalComputationManager()) {
                LoadFlowExecution loadFlowExecution = runPowSyBlLoadFlow(
                        merged,
                        selectedStrategy,
                        selectedLoadFlowParameters,
                        computationManager,
                        diagnostics);
                loadFlowResult = loadFlowExecution.result();
                securityAnalysisDcMode = loadFlowExecution.dc();
                latestLoadFlowResult = loadFlowResult;
                lineFlows = lineFlows(merged);
                if (!loadFlowResult.succeeded()) {
                    latestLoadFlowState = AnalysisStepState.FAILED;
                    latestSecurityAnalysisState = AnalysisStepState.NOT_STARTED;
                    diagnostics.add("Load flow did not converge; security analysis was not started");
                    runRepository.save(new SecurityAnalysisRunDocument(
                            current.id(),
                            current.fileImportId(),
                            SecurityAnalysisRunState.FAILED,
                            current.startedAt(),
                            null,
                            Instant.now(),
                            AnalysisStepState.FAILED,
                            AnalysisStepState.NOT_STARTED,
                            current.parameterConfigurationId(),
                            current.parameterConfigurationName(),
                            selectedStrategy,
                            selectedLoadFlowParameters,
                            selectedSecurityAnalysisParameters,
                            loadFlowResult,
                            null,
                            documents.stream().map(IidmNetworkReadDocument::id).toList(),
                            counts,
                            lineFlows,
                            List.of(),
                            bounded(diagnostics),
                            "Load flow failed; security analysis aborted"));
                    return;
                }
                latestLoadFlowState = AnalysisStepState.DONE;
                latestSecurityAnalysisState = AnalysisStepState.STARTED;
                contingencies = contingencies(merged, selectedSecurityAnalysisParameters, diagnostics);
                SecurityAnalysisReport report = runPowSyBlSecurityAnalysis(
                        merged,
                        contingencies,
                        selectedLoadFlowParameters,
                        selectedSecurityAnalysisParameters,
                        securityAnalysisDcMode,
                        computationManager,
                        diagnostics);
                computationResult = toComputationResult(report, contingencies.size());
                violations = computationResult.postContingencyViolations();
                latestSecurityAnalysisState = computationResult.succeeded() ? AnalysisStepState.DONE : AnalysisStepState.FAILED;
            }
            diagnostics.add("Security analysis evaluated " + contingencies.size() + " contingency(ies)");
            boolean securityAnalysisSucceeded = computationResult.succeeded();
            SecurityAnalysisRunDocument done = new SecurityAnalysisRunDocument(
                    current.id(),
                    current.fileImportId(),
                    securityAnalysisSucceeded ? SecurityAnalysisRunState.DONE : SecurityAnalysisRunState.FAILED,
                    current.startedAt(),
                    securityAnalysisSucceeded ? Instant.now() : null,
                    securityAnalysisSucceeded ? null : Instant.now(),
                    latestLoadFlowState,
                    latestSecurityAnalysisState,
                    current.parameterConfigurationId(),
                    current.parameterConfigurationName(),
                    selectedStrategy,
                    selectedLoadFlowParameters,
                    selectedSecurityAnalysisParameters,
                    loadFlowResult,
                    computationResult,
                    documents.stream().map(IidmNetworkReadDocument::id).toList(),
                    counts,
                    lineFlows,
                    violations,
                    bounded(diagnostics),
                    securityAnalysisSucceeded ? "Load flow and security analysis completed" : "Security analysis failed");
            runRepository.save(done);
        } catch (Exception exception) {
            diagnostics.add(rootMessage(exception));
            logger.error("{} failed to process security-analysis run {}", moduleName(), event.runId(), exception);
            runRepository.save(new SecurityAnalysisRunDocument(
                    current.id(),
                    current.fileImportId(),
                    SecurityAnalysisRunState.FAILED,
                    current.startedAt(),
                    null,
                    Instant.now(),
                    latestLoadFlowState == AnalysisStepState.DONE ? AnalysisStepState.DONE : AnalysisStepState.FAILED,
                    latestSecurityAnalysisState == AnalysisStepState.STARTED
                            || latestSecurityAnalysisState == AnalysisStepState.DONE
                            ? AnalysisStepState.FAILED
                            : AnalysisStepState.NOT_STARTED,
                    current.parameterConfigurationId(),
                    current.parameterConfigurationName(),
                    current.loadFlowStrategy(),
                    current.loadFlowParameters(),
                    current.securityAnalysisParameters(),
                    latestLoadFlowResult,
                    current.computationResult(),
                    current.iidmNetworkIds(),
                    current.networkElementCounts(),
                    current.lineFlows(),
                    current.violations(),
                    bounded(diagnostics),
                    exception.getMessage()));
        }
    }

    public CommonPage<SecurityAnalysisRunSummary> searchRuns(String runId, String runDate, String runTime, int page, int size) {
        List<SecurityAnalysisRunSummary> rows = runRepository
                .findAll(defaultsService.load().maxSearchRuns(), DocumentSort.descending("startedAt"))
                .stream()
                .map(this::toSummary)
                .filter(summary -> matches(runId, summary.runId()))
                .filter(summary -> matches(runDate, summary.runDate()))
                .filter(summary -> matches(runTime, summary.runTime()))
                .toList();
        return page(rows, page, size);
    }

    public SecurityAnalysisRunDetail runDetail(String runId) {
        SecurityAnalysisRunDocument document = findRun(runId)
                .orElseThrow(() -> new IllegalArgumentException("Security analysis run not found: " + runId));
        return new SecurityAnalysisRunDetail(
                toSummary(document),
                document.parameterConfiguration(),
                document.loadFlowResult(),
                document.computationResult(),
                document.lineFlows(),
                document.violations(),
                document.networkElementCounts(),
                document.diagnostics());
    }

    private Optional<SecurityAnalysisRunDocument> findRun(String runId) {
        return runRepository.findByField("id", runId, 1).stream().findFirst();
    }

    private List<IidmNetworkReadDocument> loadNetworkDocuments(SecurityAnalysisRequested event) {
        if (!event.iidmNetworkIds().isEmpty()) {
            List<IidmNetworkReadDocument> documents = event.iidmNetworkIds().stream()
                    .flatMap(networkId -> iidmNetworkRepository.findByField("id", networkId, 1).stream())
                    .toList();
            if (!documents.isEmpty()) {
                return documents;
            }
        }
        return iidmNetworkRepository.findByField(
                "importId",
                event.fileImportId(),
                defaultsService.load().maxIidmNetworks());
    }

    private Network readNetwork(IidmNetworkReadDocument document) {
        String xiidm = document.networkXiidm().isBlank()
                ? String.join("", document.networkXiidmChunks())
                : document.networkXiidm();
        if (xiidm.isBlank()) {
            throw new IllegalStateException("IIDM network " + document.id() + " has no XIIDM payload");
        }
        return IidmNetworkXiidm.read(xiidm);
    }

    private Network mergeInMemory(List<Network> networks, List<String> diagnostics) {
        if (networks.size() == 1) {
            diagnostics.add("One IIDM network found; merge step skipped");
            return networks.get(0);
        }
        diagnostics.add("Loaded " + networks.size() + " IIDM networks for in-memory binding");
        try {
            Class<?> mergerClass = Class.forName("com.powsybl.iidm.network.NetworkMerger");
            Object merger = mergerClass.getDeclaredConstructor().newInstance();
            Method merge = mergerClass.getMethod("merge", Network.class, Network.class);
            Network merged = networks.get(0);
            for (int index = 1; index < networks.size(); index++) {
                Object result = merge.invoke(merger, merged, networks.get(index));
                if (result instanceof Network network) {
                    merged = network;
                }
            }
            diagnostics.add("PowSyBl NetworkMerger completed in memory");
            return merged;
        } catch (ReflectiveOperationException exception) {
            diagnostics.add("PowSyBl NetworkMerger is not available in this runtime; first network used with diagnostics");
            return networks.get(0);
        }
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

    private List<LineFlow> lineFlows(Network network) {
        return network.getLineStream()
                .sorted(Comparator.comparing(Line::getId))
                .limit(defaultsService.load().maxLineFlows())
                .map(line -> {
                    double p1 = finite(line.getTerminal1().getP());
                    double q1 = finite(line.getTerminal1().getQ());
                    double loading = Math.min(150.0, Math.max(0.0, Math.abs(p1) / 10.0));
                    return new LineFlow(
                            line.getId(),
                            busId(line.getTerminal1().getBusView().getBus()),
                            busId(line.getTerminal2().getBusView().getBus()),
                            p1,
                            q1,
                            loading);
                })
                .toList();
    }

    private List<Contingency> contingencies(
            Network network,
            SecurityAnalysisParametersDto parameters,
            List<String> diagnostics) {
        int max = parameters == null
                ? defaultsService.load().securityAnalysisParameters().maxGeneratedContingencies()
                : parameters.maxGeneratedContingencies();
        String elementType = parameters == null ? "LINE" : parameters.contingencyElementType();
        List<Contingency> contingencies = network.getLineStream()
                .sorted(Comparator.comparing(Line::getId))
                .limit(max)
                .map(line -> "BRANCH".equalsIgnoreCase(elementType)
                        ? Contingency.branch(line.getId(), "N-1-" + line.getId())
                        : Contingency.line(line.getId(), "N-1-" + line.getId()))
                .filter(contingency -> contingency.isValid(network))
                .toList();
        diagnostics.add("Generated " + contingencies.size() + " " + elementType + " contingency(ies)");
        return contingencies;
    }

    private SecurityAnalysisReport runPowSyBlSecurityAnalysis(
            Network network,
            List<Contingency> contingencies,
            LoadFlowParametersDto loadFlowParameters,
            SecurityAnalysisParametersDto securityAnalysisParameters,
            boolean dc,
            LocalComputationManager computationManager,
            List<String> diagnostics) throws IOException {
        SecurityAnalysisRunParameters runParameters = new SecurityAnalysisRunParameters()
                .setSecurityAnalysisParameters(toPowSyBlParameters(loadFlowParameters, securityAnalysisParameters, dc));
        runParameters.setComputationManager(computationManager);
        diagnostics.add("Invoking PowSyBl SecurityAnalysis.run with " + (dc ? "DC" : "AC") + " load-flow parameters");
        return SecurityAnalysis.run(network, contingencies, runParameters);
    }

    private LoadFlowExecution runPowSyBlLoadFlow(
            Network network,
            LoadFlowStrategy strategy,
            LoadFlowParametersDto parameters,
            LocalComputationManager computationManager,
            List<String> diagnostics) {
        LoadFlowStrategy selectedStrategy = strategy == null ? defaultsService.load().loadFlowStrategy() : strategy;
        if (selectedStrategy == LoadFlowStrategy.AC_WITH_DC_FAILOVER) {
            LoadFlowExecution ac = runSinglePowSyBlLoadFlow(network, parameters, false, computationManager, diagnostics);
            if (ac.result().succeeded()) {
                return ac;
            }
            diagnostics.add("AC load flow failed; retrying with DC load flow");
            return runSinglePowSyBlLoadFlow(network, parameters, true, computationManager, diagnostics);
        }
        return runSinglePowSyBlLoadFlow(
                network,
                parameters,
                selectedStrategy == LoadFlowStrategy.DC_ONLY,
                computationManager,
                diagnostics);
    }

    private LoadFlowExecution runSinglePowSyBlLoadFlow(
            Network network,
            LoadFlowParametersDto parameters,
            boolean dc,
            LocalComputationManager computationManager,
            List<String> diagnostics) {
        diagnostics.add("Invoking PowSyBl LoadFlow.run in " + (dc ? "DC" : "AC") + " mode");
        LoadFlowRunParameters runParameters = new LoadFlowRunParameters()
                .setComputationManager(computationManager)
                .setParameters(toLoadFlowParameters(parameters, dc));
        com.powsybl.loadflow.LoadFlowResult result = LoadFlow.run(network, runParameters);
        List<String> componentStatuses = result.getComponentResults().stream()
                .map(component -> "component=" + component.getConnectedComponentNum()
                        + ", synchronous=" + component.getSynchronousComponentNum()
                        + ", status=" + component.getStatus()
                        + ", iterations=" + component.getIterationCount())
                .toList();
        String logs = result.getLogs();
        LoadFlowComputationResult computationResult = new LoadFlowComputationResult(
                result.isOk(),
                String.valueOf(result.getStatus()),
                result.getComponentResults().size(),
                componentStatuses,
                result.getMetrics(),
                logs == null ? "" : logs);
        return new LoadFlowExecution(computationResult, dc);
    }

    private SecurityAnalysisParameters toPowSyBlParameters(
            LoadFlowParametersDto loadFlowDto,
            SecurityAnalysisParametersDto dto,
            boolean dc) {
        SecurityAnalysisParameters parameters = new SecurityAnalysisParameters();
        SecurityAnalysisParametersDto values = dto == null ? defaultsService.load().securityAnalysisParameters() : dto;
        LoadFlowParameters loadFlowParameters = toLoadFlowParameters(loadFlowDto, dc);
        parameters
                .setLoadFlowParameters(loadFlowParameters)
                .setIntermediateResultsInOperatorStrategy(values.intermediateResultsInOperatorStrategy());
        if (!values.debugDir().isBlank()) {
            parameters.setDebugDir(values.debugDir());
        }
        return parameters;
    }

    private LoadFlowParameters toLoadFlowParameters(LoadFlowParametersDto dto, boolean dc) {
        LoadFlowParametersDto values = dto == null ? defaultsService.load().loadFlowParameters() : dto;
        return new LoadFlowParameters()
                .setDc(dc)
                .setDistributedSlack(values.distributedSlack())
                .setUseReactiveLimits(values.useReactiveLimits())
                .setTransformerVoltageControlOn(values.transformerVoltageControlOn())
                .setPhaseShifterRegulationOn(values.phaseShifterRegulationOn())
                .setShuntCompensatorVoltageControlOn(values.shuntCompensatorVoltageControlOn())
                .setReadSlackBus(values.readSlackBus())
                .setWriteSlackBus(values.writeSlackBus())
                .setHvdcAcEmulation(values.hvdcAcEmulation())
                .setDcPowerFactor(values.dcPowerFactor())
                .setVoltageInitMode(enumValue(
                        LoadFlowParameters.VoltageInitMode.class,
                        values.voltageInitMode(),
                        LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES))
                .setBalanceType(enumValue(
                        LoadFlowParameters.BalanceType.class,
                        values.balanceType(),
                        LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P))
                .setComponentMode(enumValue(
                        LoadFlowParameters.ComponentMode.class,
                        values.componentMode(),
                        LoadFlowParameters.ComponentMode.MAIN_CONNECTED));
    }

    private record LoadFlowExecution(LoadFlowComputationResult result, boolean dc) {
    }

    private SecurityAnalysisComputationResult toComputationResult(SecurityAnalysisReport report, int contingencyCount) {
        com.powsybl.security.SecurityAnalysisResult result = report.getResult();
        List<ContingencyViolation> preViolations = result.getPreContingencyLimitViolationsResult()
                .getLimitViolations()
                .stream()
                .map(violation -> toViolation("BASE", violation))
                .toList();
        List<String> postStatuses = result.getPostContingencyResults().stream()
                .map(post -> post.getContingency().getId() + "=" + post.getStatus())
                .toList();
        List<ContingencyViolation> postViolations = result.getPostContingencyResults().stream()
                .flatMap(post -> post.getLimitViolationsResult().getLimitViolations().stream()
                        .map(violation -> toViolation(post.getContingency().getId(), violation)))
                .toList();
        boolean succeeded = result.getPreContingencyLimitViolationsResult().isComputationOk()
                && result.getPostContingencyResults().stream()
                        .allMatch(post -> post.getStatus() == com.powsybl.security.PostContingencyComputationStatus.CONVERGED
                                || post.getStatus() == com.powsybl.security.PostContingencyComputationStatus.NO_IMPACT);
        String preStatus = result.getPreContingencyResult() == null ? "" : String.valueOf(result.getPreContingencyResult().getStatus());
        return new SecurityAnalysisComputationResult(
                succeeded,
                preStatus,
                contingencyCount,
                postStatuses,
                preViolations,
                postViolations);
    }

    private ContingencyViolation toViolation(String contingencyId, LimitViolation violation) {
        return new ContingencyViolation(
                contingencyId,
                violation.getSubjectId(),
                violationType(violation.getLimitType().name()),
                finite(violation.getValue()),
                finite(violation.getLimit()),
                unit(violation.getLimitType().name()),
                severity(violation.getValue(), violation.getLimit()));
    }

    private List<ContingencyViolation> violations(List<LineFlow> lineFlows) {
        return lineFlows.stream()
                .filter(flow -> flow.loadingPercent() > 100.0)
                .map(flow -> new ContingencyViolation(
                        "BASE",
                        flow.elementId(),
                        ViolationType.OVERLOAD,
                        flow.loadingPercent(),
                        100.0,
                        "%",
                        flow.loadingPercent() > 120.0 ? "HIGH" : "MEDIUM"))
                .toList();
    }

    private LfSaParameterConfiguration resolveParameterConfiguration(String id) {
        if (id == null || id.isBlank()) {
            return defaultParameterConfiguration();
        }
        return parameterRepository.findByField("id", id, 1).stream()
                .findFirst()
                .map(this::toParameterConfiguration)
                .orElse(defaultParameterConfiguration());
    }

    private LfSaParameterConfiguration toParameterConfiguration(
            SecurityAnalysisParameterConfigurationDocument document) {
        LfSaDefaults defaults = defaultsService.load();
        return new LfSaParameterConfiguration(
                document.id(),
                document.name(),
                document.source(),
                instantString(document.createdAt()),
                instantString(document.updatedAt()),
                document.loadFlowStrategy() == null ? defaults.loadFlowStrategy() : document.loadFlowStrategy(),
                document.loadFlowParameters() == null ? defaults.loadFlowParameters() : document.loadFlowParameters(),
                document.securityAnalysisParameters() == null
                        ? defaults.securityAnalysisParameters()
                        : document.securityAnalysisParameters());
    }

    private String busId(Bus bus) {
        return bus == null ? "" : bus.getId();
    }

    private double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private SecurityAnalysisImportCandidate toCandidate(CnmImportReadDocument document) {
        String businessDay = document.files().stream()
                .map(CnmImportReadDocument.CnmImportFileReadDocument::businessDay)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
        return new SecurityAnalysisImportCandidate(
                document.id(),
                value(document.serviceType()),
                displayTimeFrame(value(document.timeFrame())),
                value(document.state()),
                instantString(document.createdAt()),
                businessDay,
                document.message());
    }

    private boolean isAnalysisReadyImport(CnmImportReadDocument document) {
        return document.iidmTransformationStatus() == IidmTransformationStatus.DONE
                && document.state() == ImportState.SUCCESS;
    }

    private SecurityAnalysisRunSummary toSummary(SecurityAnalysisRunDocument document) {
        Instant startedAt = instant(document.startedAt()).orElse(Instant.now());
        return new SecurityAnalysisRunSummary(
                document.id(),
                document.fileImportId(),
                document.state(),
                document.loadFlowState(),
                document.securityAnalysisState(),
                DATE.format(startedAt.atZone(ZoneOffset.UTC)),
                TIME.format(startedAt.atZone(ZoneOffset.UTC)),
                document.iidmNetworkIds().size(),
                document.lineFlows().size(),
                document.violations().size(),
                document.diagnostics().size(),
                document.message());
    }

    private <T> CommonPage<T> page(List<T> rows, int page, int size) {
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int from = Math.min(rows.size(), safePage * safeSize);
        int to = Math.min(rows.size(), from + safeSize);
        return new CommonPage<>(rows.subList(from, to), rows.size(), safePage, safeSize);
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

    private ViolationType violationType(String limitType) {
        return switch (limitType) {
            case "LOW_VOLTAGE" -> ViolationType.VOLTAGE_LOW;
            case "HIGH_VOLTAGE" -> ViolationType.VOLTAGE_HIGH;
            default -> ViolationType.OVERLOAD;
        };
    }

    private String unit(String limitType) {
        return limitType.contains("VOLTAGE") ? "kV" : "%";
    }

    private String severity(double value, double limit) {
        double denominator = Math.abs(limit) < 0.0001 ? 1.0 : Math.abs(limit);
        double ratio = Math.abs(value - limit) / denominator;
        return ratio > 0.2 ? "HIGH" : "MEDIUM";
    }

    private boolean matches(String filter, String value) {
        return filter == null
                || filter.isBlank()
                || (value != null && value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)));
    }

    private boolean matchesDate(String date, SecurityAnalysisImportCandidate candidate) {
        return date == null
                || date.isBlank()
                || date.equals(candidate.businessDay())
                || candidate.createdAt().startsWith(date);
    }

    private String displayTimeFrame(String timeFrame) {
        return "DAY_AHEAD".equals(timeFrame) ? "DAY AHEAD" : timeFrame.replace('_', ' ');
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private String id(String requestId, String prefix) {
        return requestId == null || requestId.isBlank() ? prefix + "-" + UUID.randomUUID() : requestId + "-" + prefix;
    }

    private String contingency(SecurityAnalysisRequest request, int index) {
        return request.contingencyIds().size() > index ? request.contingencyIds().get(index) : "N-1-" + (index + 1);
    }

    private List<String> bounded(List<String> diagnostics) {
        int maxDiagnostics = defaultsService.load().maxDiagnostics();
        if (diagnostics.size() <= maxDiagnostics) {
            return List.copyOf(diagnostics);
        }
        return List.copyOf(diagnostics.subList(0, maxDiagnostics));
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private String instantString(Object value) {
        return instant(value).map(Instant::toString).orElse("");
    }

    private Optional<Instant> instant(Object value) {
        if (value instanceof Instant instant) {
            return Optional.of(instant);
        }
        if (value instanceof Number number) {
            long epoch = number.longValue();
            return Optional.of(epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch));
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Optional.of(Instant.parse(string));
            } catch (RuntimeException ignored) {
                try {
                    return Optional.of(LocalDate.parse(string, DATE).atStartOfDay().toInstant(ZoneOffset.UTC));
                } catch (RuntimeException ignoredAgain) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}
