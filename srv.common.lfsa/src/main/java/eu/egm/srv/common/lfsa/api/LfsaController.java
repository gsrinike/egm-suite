package eu.egm.srv.common.lfsa.api;

import eu.egm.data.common.LoadFlowRequest;
import eu.egm.data.common.LoadFlowResult;
import eu.egm.data.common.SecurityAnalysisRequest;
import eu.egm.data.common.SecurityAnalysisResult;
import eu.egm.srv.common.lfsa.service.LfsaService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common/lfsa")
public class LfsaController {
    private final LfsaService service;

    public LfsaController(LfsaService service) {
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
}
