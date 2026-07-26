package eu.egm.srv.common.lfsa.api.sensitivity;

import eu.egm.data.common.lfsa.common.CommonPage;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisConfiguration;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisConfigurationSaveRequest;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunDetail;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunStartRequest;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunSummary;
import eu.egm.data.common.lfsa.sensitivity.SensitivityIidmNetworkSummary;
import eu.egm.data.common.lfsa.sensitivity.SensitivityInputTable;
import eu.egm.data.common.lfsa.sensitivity.SensitivityInputUploadResponse;
import eu.egm.srv.common.lfsa.service.sensitivity.SensitivityAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST API for CGM sensitivity-analysis configuration, execution, and results.
 */
@RestController
@RequestMapping("/api/common/lfsa/sensitivity")
public class SensitivityAnalysisController {
    private final SensitivityAnalysisService sensitivityService;

    public SensitivityAnalysisController(SensitivityAnalysisService sensitivityService) {
        this.sensitivityService = sensitivityService;
    }

    @GetMapping("/iidm-networks")
    public CommonPage<SensitivityIidmNetworkSummary> sensitivityIidmNetworks(
            @RequestParam String importId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return sensitivityService.completedIidmNetworks(importId, page, size);
    }

    @GetMapping("/configurations/default")
    public SensitivityAnalysisConfiguration defaultSensitivityConfiguration() {
        return sensitivityService.defaultConfiguration();
    }

    @GetMapping("/configurations")
    public CommonPage<SensitivityAnalysisConfiguration> sensitivityConfigurations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return sensitivityService.configurations(page, size);
    }

    @PostMapping(value = "/configurations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SensitivityAnalysisConfiguration saveSensitivityConfiguration(
            @RequestBody SensitivityAnalysisConfigurationSaveRequest request) {
        return sensitivityService.saveConfiguration(request);
    }

    @PostMapping(value = "/inputs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SensitivityInputUploadResponse uploadSensitivityInput(
            @RequestParam String kind,
            @RequestParam("file") MultipartFile file) {
        return sensitivityService.uploadInput(kind, file);
    }

    @PostMapping(value = "/runs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SensitivityAnalysisRunSummary startSensitivityRun(@RequestBody SensitivityAnalysisRunStartRequest request) {
        return sensitivityService.startRun(request);
    }

    @GetMapping("/runs")
    public CommonPage<SensitivityAnalysisRunSummary> searchSensitivityRuns(
            @RequestParam(required = false) String runId,
            @RequestParam(required = false) String runDate,
            @RequestParam(required = false) String runTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return sensitivityService.searchRuns(runId, runDate, runTime, page, size);
    }

    @GetMapping("/runs/{runId}")
    public SensitivityAnalysisRunDetail sensitivityRunDetail(@PathVariable String runId) {
        return sensitivityService.detail(runId);
    }

    @GetMapping("/runs/{runId}/inputs/{kind}/table")
    public SensitivityInputTable sensitivityInputTable(
            @PathVariable String runId,
            @PathVariable String kind) {
        return sensitivityService.inputTable(runId, kind);
    }
}
