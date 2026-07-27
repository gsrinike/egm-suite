package eu.egm.srv.common.lfsa.api;

import eu.egm.data.common.lfsa.common.CommonPage;
import eu.egm.data.common.lfsa.common.LfSaParameterConfiguration;
import eu.egm.data.common.lfsa.common.LfSaParameterConfigurationSaveRequest;
import eu.egm.data.common.lfsa.common.LoadFlowRequest;
import eu.egm.data.common.lfsa.common.LoadFlowResult;
import eu.egm.data.common.lfsa.common.SecurityAnalysisImportCandidate;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRequest;
import eu.egm.data.common.lfsa.common.SecurityAnalysisResult;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunDetail;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunStartRequest;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunSummary;
import eu.egm.srv.common.lfsa.service.LfSaService;
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
public class LfSaController {
    private final LfSaService service;

    public LfSaController(LfSaService service) {
        this.service = service;
    }

    @PostMapping(value = "/load-flow", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoadFlowResult runLoadFlow(@RequestBody LoadFlowRequest request) {
        return service.runLoadFlow(request);
    }

    @PostMapping(value = "/security-analysis", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SecurityAnalysisResult runSecurityAnalysis(@RequestBody SecurityAnalysisRequest request) {
        return service.runSecurityAnalysis(request);
    }

    @GetMapping("/imports")
    public CommonPage<SecurityAnalysisImportCandidate> searchSuccessfulImports(
            @RequestParam(name = "service", required = false) String serviceName,
            @RequestParam(required = false) String timeFrame,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.searchSuccessfulImports(serviceName, timeFrame, date, page, size);
    }

    @PostMapping(value = "/security-analysis/runs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SecurityAnalysisRunSummary startSecurityAnalysis(@RequestBody SecurityAnalysisRunStartRequest request) {
        return service.startSecurityAnalysis(request);
    }

    @GetMapping("/security-analysis/parameters/default")
    public LfSaParameterConfiguration defaultSecurityAnalysisParameters() {
        return service.defaultParameterConfiguration();
    }

    @GetMapping("/security-analysis/parameters")
    public CommonPage<LfSaParameterConfiguration> securityAnalysisParameters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.parameterConfigurations(page, size);
    }

    @PostMapping(value = "/security-analysis/parameters", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LfSaParameterConfiguration saveSecurityAnalysisParameters(
            @RequestBody LfSaParameterConfigurationSaveRequest request) {
        return service.saveParameterConfiguration(request);
    }

    @GetMapping("/security-analysis/runs")
    public CommonPage<SecurityAnalysisRunSummary> searchRuns(
            @RequestParam(required = false) String runId,
            @RequestParam(required = false) String runDate,
            @RequestParam(required = false) String runTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.searchRuns(runId, runDate, runTime, page, size);
    }

    @GetMapping("/security-analysis/runs/{runId}")
    public SecurityAnalysisRunDetail runDetail(@PathVariable String runId) {
        return service.runDetail(runId);
    }
}
