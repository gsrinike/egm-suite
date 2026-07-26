package eu.egm.srv.common.lfsa.service;

import com.infra.InfrastructureUtils;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSort;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.utils.restservice.RestServiceSupport;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.common.CommonPage;
import eu.egm.data.common.ContingencyViolation;
import eu.egm.data.common.LineFlow;
import eu.egm.data.common.LoadFlowRequest;
import eu.egm.data.common.LoadFlowResult;
import eu.egm.data.common.SecurityAnalysisImportCandidate;
import eu.egm.data.common.SecurityAnalysisRequest;
import eu.egm.data.common.SecurityAnalysisRequested;
import eu.egm.data.common.SecurityAnalysisResult;
import eu.egm.data.common.SecurityAnalysisRunDetail;
import eu.egm.data.common.SecurityAnalysisRunStartRequest;
import eu.egm.data.common.SecurityAnalysisRunState;
import eu.egm.data.common.SecurityAnalysisRunSummary;
import eu.egm.data.common.ViolationType;
import eu.egm.data.common.WorkflowStatus;
import eu.egm.data.iidm.network.IidmNetworkXiidm;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocument;
import eu.egm.srv.common.lfsa.domain.CnmImportReadDocumentAdapter;
import eu.egm.srv.common.lfsa.domain.IidmNetworkReadDocument;
import eu.egm.srv.common.lfsa.domain.IidmNetworkReadDocumentAdapter;
import eu.egm.srv.common.lfsa.domain.SecurityAnalysisRunDocument;
import eu.egm.srv.common.lfsa.domain.SecurityAnalysisRunDocumentAdapter;
import io.micrometer.observation.ObservationRegistry;
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
    private static final int MAX_SEARCH_IMPORTS = 1000;
    private static final int MAX_SEARCH_RUNS = 1000;
    private static final int MAX_DIAGNOSTICS = 50;

    private final InfrastructureUtils infrastructureUtils;
    private final DocumentRepositoryService<CnmImportReadDocument> importRepository;
    private final DocumentRepositoryService<IidmNetworkReadDocument> iidmNetworkRepository;
    private final DocumentRepositoryService<SecurityAnalysisRunDocument> runRepository;
    private final String exchange;
    private final String routingKey;

    public LfSaService(
            Environment environment,
            ObservationRegistry observationRegistry,
            InfrastructureUtils infrastructureUtils,
            @Value("${lfsa.security-analysis.event.exchange:lfsa.events}") String exchange,
            @Value("${lfsa.security-analysis.event.requested-routing-key:lfsa.security-analysis.requested}") String routingKey) {
        super(environment, observationRegistry);
        this.infrastructureUtils = infrastructureUtils;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.importRepository = infrastructureUtils.documentRepository(new CnmImportReadDocumentAdapter());
        this.iidmNetworkRepository = infrastructureUtils.documentRepository(new IidmNetworkReadDocumentAdapter());
        this.runRepository = infrastructureUtils.documentRepository(new SecurityAnalysisRunDocumentAdapter());
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
        List<SecurityAnalysisImportCandidate> rows = importRepository
                .findAll(MAX_SEARCH_IMPORTS, DocumentSort.descending("createdAt"))
                .stream()
                .filter(importDocument -> importDocument.state() == ImportState.SUCCESS)
                .filter(importDocument -> matches(service, value(importDocument.serviceType())))
                .filter(importDocument -> matches(timeFrame, value(importDocument.timeFrame())))
                .map(this::toCandidate)
                .filter(candidate -> matchesDate(date, candidate))
                .toList();
        return page(rows, page, size);
    }

    public SecurityAnalysisRunSummary startSecurityAnalysis(SecurityAnalysisRunStartRequest request) {
        String importId = requireValue(request.fileImportId(), "fileImportId");
        List<IidmNetworkReadDocument> networks = iidmNetworkRepository.findByField("importId", importId, 500);
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        SecurityAnalysisRunDocument document = new SecurityAnalysisRunDocument(
                runId,
                importId,
                SecurityAnalysisRunState.STARTED,
                now,
                null,
                null,
                networks.stream().map(IidmNetworkReadDocument::id).toList(),
                Map.of(),
                List.of(),
                List.of(),
                List.of("Security analysis queued for " + networks.size() + " IIDM network document(s)"),
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
                        event.iidmNetworkIds(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of("Run document was recreated from the event payload"),
                        "Security analysis started"));
        List<String> diagnostics = new ArrayList<>(current.diagnostics());
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
            List<LineFlow> lineFlows = lineFlows(merged);
            List<ContingencyViolation> violations = violations(lineFlows);
            diagnostics.add("Security analysis evaluated " + lineFlows.size() + " line flow row(s)");
            SecurityAnalysisRunDocument done = new SecurityAnalysisRunDocument(
                    current.id(),
                    current.fileImportId(),
                    SecurityAnalysisRunState.DONE,
                    current.startedAt(),
                    Instant.now(),
                    null,
                    documents.stream().map(IidmNetworkReadDocument::id).toList(),
                    counts,
                    lineFlows,
                    violations,
                    bounded(diagnostics),
                    "Security analysis completed");
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
                .findAll(MAX_SEARCH_RUNS, DocumentSort.descending("startedAt"))
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
        return iidmNetworkRepository.findByField("importId", event.fileImportId(), 500);
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
                .limit(500)
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

    private SecurityAnalysisRunSummary toSummary(SecurityAnalysisRunDocument document) {
        Instant startedAt = instant(document.startedAt()).orElse(Instant.now());
        return new SecurityAnalysisRunSummary(
                document.id(),
                document.fileImportId(),
                document.state(),
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
        if (diagnostics.size() <= MAX_DIAGNOSTICS) {
            return List.copyOf(diagnostics);
        }
        return List.copyOf(diagnostics.subList(0, MAX_DIAGNOSTICS));
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
