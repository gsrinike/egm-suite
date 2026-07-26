package eu.egm.mock.srv.common.lfsa.api;

import eu.egm.data.common.lfsa.common.CommonPage;
import eu.egm.data.common.lfsa.common.ContingencyViolation;
import eu.egm.data.common.lfsa.common.AnalysisStepState;
import eu.egm.data.common.lfsa.common.LfSaParameterConfiguration;
import eu.egm.data.common.lfsa.common.LfSaParameterConfigurationSaveRequest;
import eu.egm.data.common.lfsa.common.LineFlow;
import eu.egm.data.common.lfsa.common.LoadFlowComputationResult;
import eu.egm.data.common.lfsa.common.LoadFlowParametersDto;
import eu.egm.data.common.lfsa.common.LoadFlowRequest;
import eu.egm.data.common.lfsa.common.LoadFlowResult;
import eu.egm.data.common.lfsa.common.LoadFlowStrategy;
import eu.egm.data.common.lfsa.common.SecurityAnalysisComputationResult;
import eu.egm.data.common.lfsa.common.SecurityAnalysisImportCandidate;
import eu.egm.data.common.lfsa.common.SecurityAnalysisParametersDto;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRequest;
import eu.egm.data.common.lfsa.common.SecurityAnalysisResult;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunDetail;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunStartRequest;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunState;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunSummary;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisConfiguration;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisConfigurationSaveRequest;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisParametersDto;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunDetail;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunStartRequest;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunState;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunSummary;
import eu.egm.data.common.lfsa.sensitivity.SensitivityFactorDto;
import eu.egm.data.common.lfsa.sensitivity.SensitivityInputTable;
import eu.egm.data.common.lfsa.sensitivity.SensitivityInputUploadResponse;
import eu.egm.data.common.lfsa.sensitivity.SensitivityMatrixRow;
import eu.egm.data.common.lfsa.common.ViolationType;
import eu.egm.data.common.lfsa.common.WorkflowStatus;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/common/lfsa")
public class MockLfSaController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final Map<String, SecurityAnalysisRunDetail> runs = new ConcurrentHashMap<>();
    private final Map<String, LfSaParameterConfiguration> parameters = new ConcurrentHashMap<>();
    private final Map<String, SensitivityAnalysisRunDetail> sensitivityRuns = new ConcurrentHashMap<>();
    private final Map<String, SensitivityAnalysisConfiguration> sensitivityConfigurations = new ConcurrentHashMap<>();

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

    @GetMapping("/sensitivity/iidm-networks")
    public CommonPage<Map<String, Object>> sensitivityIidmNetworks(
            @RequestParam String importId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<Map<String, Object>> rows = List.of(
                Map.of(
                        "id", importId + ":mock-network-1",
                        "importId", importId,
                        "sourceFileIds", List.of("mock-eq", "mock-ssh"),
                        "sourceFileNames", List.of("MOCK_EQ.zip", "MOCK_SSH.zip"),
                        "businessDay", "2024-12-03",
                        "businessTime", "10:30",
                        "timeFrame", "DAY AHEAD",
                        "tsoName", "TSO-XYZ",
                        "networkFormat", "XIIDM"));
        return new CommonPage<>(rows, rows.size(), page, size);
    }

    @GetMapping("/sensitivity/configurations/default")
    public SensitivityAnalysisConfiguration defaultSensitivityConfiguration() {
        return new SensitivityAnalysisConfiguration("", "Default Sensitivity", "DEFAULT", "", "", defaultSensitivityParameters());
    }

    @GetMapping("/sensitivity/configurations")
    public CommonPage<SensitivityAnalysisConfiguration> sensitivityConfigurations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<SensitivityAnalysisConfiguration> rows = new ArrayList<>(sensitivityConfigurations.values());
        return new CommonPage<>(rows, rows.size(), page, size);
    }

    @PostMapping(value = "/sensitivity/configurations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SensitivityAnalysisConfiguration saveSensitivityConfiguration(
            @RequestBody SensitivityAnalysisConfigurationSaveRequest request) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        SensitivityAnalysisConfiguration configuration = new SensitivityAnalysisConfiguration(
                id,
                request.name() == null || request.name().isBlank()
                        ? DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(now)
                                + "_SENS_Conf"
                        : request.name(),
                "USER",
                now.toString(),
                now.toString(),
                request.parameters() == null ? defaultSensitivityParameters() : request.parameters());
        sensitivityConfigurations.put(id, configuration);
        return configuration;
    }

    @PostMapping(value = "/sensitivity/inputs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SensitivityInputUploadResponse uploadSensitivityInput(
            @RequestParam String kind,
            @RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename() == null ? "input.dat" : file.getOriginalFilename();
        return new SensitivityInputUploadResponse(
                kind.toUpperCase(),
                fileName,
                "mock-sensitivity-inputs/" + kind.toLowerCase() + "/" + UUID.randomUUID() + "-" + fileName,
                file.getSize());
    }

    @PostMapping(value = "/sensitivity/runs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SensitivityAnalysisRunSummary startSensitivityRun(@RequestBody SensitivityAnalysisRunStartRequest request) {
        Instant now = Instant.now();
        String runId = UUID.randomUUID().toString();
        List<SensitivityFactorDto> factors = List.of(
                new SensitivityFactorDto("BRANCH_ACTIVE_POWER_1", "MOCK-LINE-1", "INJECTION_ACTIVE_POWER", "MOCK-GEN-1", "ALL"));
        List<SensitivityMatrixRow> matrix = List.of(
                new SensitivityMatrixRow("BRANCH_ACTIVE_POWER_1", "MOCK-LINE-1", "INJECTION_ACTIVE_POWER", "MOCK-GEN-1", "BASE", 0.42, 120.0));
        SensitivityAnalysisRunSummary summary = new SensitivityAnalysisRunSummary(
                runId,
                request.fileImportId(),
                SensitivityAnalysisRunState.DONE,
                DATE.format(now.atZone(ZoneOffset.UTC)),
                TIME.format(now.atZone(ZoneOffset.UTC)),
                request.iidmNetworkIds().size(),
                factors.size(),
                matrix.size(),
                1,
                request.ptdfObjectId(),
                request.lodfObjectId(),
                request.glskObjectId(),
                "Mock sensitivity analysis completed");
        sensitivityRuns.put(runId, new SensitivityAnalysisRunDetail(
                summary,
                sensitivityConfiguration(request.configurationId()),
                request.iidmNetworkIds(),
                Map.of("ptdfObjectId", request.ptdfObjectId(), "lodfObjectId", request.lodfObjectId(), "glskObjectId", request.glskObjectId()),
                factors,
                matrix,
                Map.of("lines", 1L, "generators", 1L),
                List.of("Mock sensitivity result generated")));
        return summary;
    }

    @GetMapping("/sensitivity/runs")
    public CommonPage<SensitivityAnalysisRunSummary> searchSensitivityRuns(
            @RequestParam(required = false) String runId,
            @RequestParam(required = false) String runDate,
            @RequestParam(required = false) String runTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<SensitivityAnalysisRunSummary> rows = new ArrayList<>(sensitivityRuns.values().stream().map(SensitivityAnalysisRunDetail::summary).toList());
        List<SensitivityAnalysisRunSummary> filtered = rows.stream()
                .filter(row -> runId == null || runId.isBlank() || row.runId().contains(runId))
                .filter(row -> runDate == null || runDate.isBlank() || row.runDate().equals(runDate))
                .filter(row -> runTime == null || runTime.isBlank() || row.runTime().contains(runTime))
                .toList();
        return new CommonPage<>(filtered, filtered.size(), page, size);
    }

    @GetMapping("/sensitivity/runs/{runId}")
    public SensitivityAnalysisRunDetail sensitivityRunDetail(@PathVariable String runId) {
        SensitivityAnalysisRunDetail detail = sensitivityRuns.get(runId);
        if (detail == null) {
            throw new IllegalArgumentException("Mock sensitivity run not found: " + runId);
        }
        return detail;
    }

    @GetMapping("/sensitivity/runs/{runId}/inputs/{kind}/table")
    public SensitivityInputTable sensitivityInputTable(
            @PathVariable String runId,
            @PathVariable String kind) {
        return new SensitivityInputTable(
                kind.toUpperCase(),
                "mock-sensitivity-inputs/" + kind.toLowerCase() + "/" + runId,
                List.of(
                        Map.of("source", "zone-a", "target", "zone-b", "value", 0.42),
                        Map.of("source", "zone-b", "target", "zone-c", "value", -0.13)));
    }

    private LfSaParameterConfiguration parameterConfiguration(String id) {
        if (id == null || id.isBlank()) {
            return defaultSecurityAnalysisParameters();
        }
        return parameters.getOrDefault(id, defaultSecurityAnalysisParameters());
    }

    private SensitivityAnalysisConfiguration sensitivityConfiguration(String id) {
        if (id == null || id.isBlank()) {
            return defaultSensitivityConfiguration();
        }
        return sensitivityConfigurations.getOrDefault(id, defaultSensitivityConfiguration());
    }

    private SensitivityAnalysisParametersDto defaultSensitivityParameters() {
        return new SensitivityAnalysisParametersDto(
                true,
                "BRANCH_ACTIVE_POWER_1",
                "INJECTION_ACTIVE_POWER",
                "ALL",
                25,
                25,
                25,
                0.0,
                0.0,
                0.0,
                0.0,
                "NONE",
                "");
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
