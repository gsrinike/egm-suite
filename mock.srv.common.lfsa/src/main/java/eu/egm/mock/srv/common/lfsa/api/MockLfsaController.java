package eu.egm.mock.srv.common.lfsa.api;

import eu.egm.data.common.CommonPage;
import eu.egm.data.common.ContingencyViolation;
import eu.egm.data.common.AnalysisStepState;
import eu.egm.data.common.LfSaParameterConfiguration;
import eu.egm.data.common.LfSaParameterConfigurationSaveRequest;
import eu.egm.data.common.LineFlow;
import eu.egm.data.common.LoadFlowComputationResult;
import eu.egm.data.common.LoadFlowParametersDto;
import eu.egm.data.common.LoadFlowRequest;
import eu.egm.data.common.LoadFlowResult;
import eu.egm.data.common.LoadFlowStrategy;
import eu.egm.data.common.SecurityAnalysisComputationResult;
import eu.egm.data.common.SecurityAnalysisImportCandidate;
import eu.egm.data.common.SecurityAnalysisParametersDto;
import eu.egm.data.common.SecurityAnalysisRequest;
import eu.egm.data.common.SecurityAnalysisResult;
import eu.egm.data.common.SecurityAnalysisRunDetail;
import eu.egm.data.common.SecurityAnalysisRunStartRequest;
import eu.egm.data.common.SecurityAnalysisRunState;
import eu.egm.data.common.SecurityAnalysisRunSummary;
import eu.egm.data.common.ViolationType;
import eu.egm.data.common.WorkflowStatus;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common/lfsa")
public class MockLfSaController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final Map<String, SecurityAnalysisRunDetail> runs = new ConcurrentHashMap<>();
    private final Map<String, LfSaParameterConfiguration> parameters = new ConcurrentHashMap<>();

    @PostMapping(value = "/load-flow", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoadFlowResult runLoadFlow(@RequestBody LoadFlowRequest request) {
        return new LoadFlowResult(
                "mock-lf-" + request.requestId(),
                WorkflowStatus.COMPLETED,
                List.of(new LineFlow("MOCK-LINE-1", "BUS-A", "BUS-B", 200.0, 30.0, 88.5)),
                Instant.now(),
                "Mock load flow completed");
    }

    @PostMapping(value = "/security-analysis", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SecurityAnalysisResult runSecurityAnalysis(@RequestBody SecurityAnalysisRequest request) {
        return new SecurityAnalysisResult(
                "mock-sa-" + request.requestId(),
                WorkflowStatus.COMPLETED,
                List.of(),
                List.of(new ContingencyViolation("MOCK-N-1", "MOCK-LINE-2", ViolationType.OVERLOAD, 115.0, 100.0, "%", "HIGH")),
                Instant.now(),
                "Mock security analysis completed");
    }

    @GetMapping("/imports")
    public CommonPage<SecurityAnalysisImportCandidate> searchSuccessfulImports(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String timeFrame,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<SecurityAnalysisImportCandidate> rows = List.of(
                new SecurityAnalysisImportCandidate("mock-import-1", "CGM", "DAY AHEAD", "SUCCESS", Instant.now().toString(), "2024-12-03", "Mock import ready"),
                new SecurityAnalysisImportCandidate("mock-import-2", "CGM", "INTRA DAY", "SUCCESS", Instant.now().toString(), "2024-12-03", "Mock intraday import ready"));
        List<SecurityAnalysisImportCandidate> filtered = rows.stream()
                .filter(row -> service == null || service.isBlank() || row.service().contains(service))
                .filter(row -> timeFrame == null || timeFrame.isBlank() || row.timeFrame().contains(timeFrame))
                .filter(row -> date == null || date.isBlank() || row.businessDay().equals(date))
                .toList();
        return new CommonPage<>(filtered, filtered.size(), page, size);
    }

    @PostMapping(value = "/security-analysis/runs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SecurityAnalysisRunSummary startSecurityAnalysis(@RequestBody SecurityAnalysisRunStartRequest request) {
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        List<LineFlow> flows = List.of(
                new LineFlow("MOCK-LINE-1", "BUS-A", "BUS-B", 120.0, 10.0, 75.0),
                new LineFlow("MOCK-LINE-2", "BUS-B", "BUS-C", 260.0, 40.0, 115.0));
        List<ContingencyViolation> violations = List.of(new ContingencyViolation("BASE", "MOCK-LINE-2", ViolationType.OVERLOAD, 115.0, 100.0, "%", "HIGH"));
        SecurityAnalysisRunSummary summary = new SecurityAnalysisRunSummary(
                runId,
                request.fileImportId(),
                SecurityAnalysisRunState.DONE,
                AnalysisStepState.DONE,
                AnalysisStepState.DONE,
                DATE.format(now.atZone(ZoneOffset.UTC)),
                TIME.format(now.atZone(ZoneOffset.UTC)),
                2,
                flows.size(),
                violations.size(),
                1,
                "Mock security analysis completed");
        LfSaParameterConfiguration parameterConfiguration = parameterConfiguration(request.parameterConfigurationId());
        LoadFlowComputationResult loadFlowResult = new LoadFlowComputationResult(
                true,
                "FULLY_CONVERGED",
                1,
                List.of("component=0, synchronous=0, status=CONVERGED, iterations=3"),
                Map.of("realLosses", "12.4"),
                "");
        SecurityAnalysisComputationResult computationResult = new SecurityAnalysisComputationResult(
                true,
                "CONVERGED",
                2,
                List.of("MOCK-N-1=CONVERGED"),
                List.of(),
                violations);
        runs.put(runId, new SecurityAnalysisRunDetail(
                summary,
                parameterConfiguration,
                loadFlowResult,
                computationResult,
                flows,
                violations,
                Map.of("lines", 2L, "buses", 3L),
                List.of("Mock PowSyBl run completed")));
        return summary;
    }

    @GetMapping("/security-analysis/parameters/default")
    public LfSaParameterConfiguration defaultSecurityAnalysisParameters() {
        return new LfSaParameterConfiguration(
                "",
                "Default LFnSA",
                "DEFAULT",
                "",
                "",
                LoadFlowStrategy.DC_ONLY,
                defaultLoadFlowParameters(),
                defaultSecurityAnalysisParametersDto());
    }

    @GetMapping("/security-analysis/parameters")
    public CommonPage<LfSaParameterConfiguration> securityAnalysisParameters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<LfSaParameterConfiguration> rows = new ArrayList<>(parameters.values());
        return new CommonPage<>(rows, rows.size(), page, size);
    }

    @PostMapping(value = "/security-analysis/parameters", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LfSaParameterConfiguration saveSecurityAnalysisParameters(
            @RequestBody LfSaParameterConfigurationSaveRequest request) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        LfSaParameterConfiguration configuration = new LfSaParameterConfiguration(
                id,
                request.name() == null || request.name().isBlank()
                        ? DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(now)
                                + "_SA_Conf"
                        : request.name(),
                "USER",
                now.toString(),
                now.toString(),
                request.loadFlowStrategy() == null ? LoadFlowStrategy.DC_ONLY : request.loadFlowStrategy(),
                request.loadFlowParameters() == null ? defaultLoadFlowParameters() : request.loadFlowParameters(),
                request.securityAnalysisParameters() == null
                        ? defaultSecurityAnalysisParametersDto()
                        : request.securityAnalysisParameters());
        parameters.put(id, configuration);
        return configuration;
    }

    @GetMapping("/security-analysis/runs")
    public CommonPage<SecurityAnalysisRunSummary> searchRuns(
            @RequestParam(required = false) String runId,
            @RequestParam(required = false) String runDate,
            @RequestParam(required = false) String runTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<SecurityAnalysisRunSummary> rows = new ArrayList<>(runs.values().stream().map(SecurityAnalysisRunDetail::summary).toList());
        List<SecurityAnalysisRunSummary> filtered = rows.stream()
                .filter(row -> runId == null || runId.isBlank() || row.runId().contains(runId))
                .filter(row -> runDate == null || runDate.isBlank() || row.runDate().equals(runDate))
                .filter(row -> runTime == null || runTime.isBlank() || row.runTime().contains(runTime))
                .toList();
        return new CommonPage<>(filtered, filtered.size(), page, size);
    }

    @GetMapping("/security-analysis/runs/{runId}")
    public SecurityAnalysisRunDetail runDetail(@PathVariable String runId) {
        SecurityAnalysisRunDetail detail = runs.get(runId);
        if (detail == null) {
            throw new IllegalArgumentException("Mock run not found: " + runId);
        }
        return detail;
    }

    private LfSaParameterConfiguration parameterConfiguration(String id) {
        if (id == null || id.isBlank()) {
            return defaultSecurityAnalysisParameters();
        }
        return parameters.getOrDefault(id, defaultSecurityAnalysisParameters());
    }

    private LoadFlowParametersDto defaultLoadFlowParameters() {
        return new LoadFlowParametersDto(
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                "PREVIOUS_VALUES",
                "PROPORTIONAL_TO_GENERATION_P",
                "MAIN_CONNECTED",
                true,
                1.0);
    }

    private SecurityAnalysisParametersDto defaultSecurityAnalysisParametersDto() {
        return new SecurityAnalysisParametersDto(
                true,
                true,
                true,
                false,
                "",
                "LINE",
                25);
    }
}
