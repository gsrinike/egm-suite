package eu.egm.mock.srv.common.lfsa.api;

import eu.egm.data.common.ContingencyViolation;
import eu.egm.data.common.LineFlow;
import eu.egm.data.common.LoadFlowRequest;
import eu.egm.data.common.LoadFlowResult;
import eu.egm.data.common.SecurityAnalysisRequest;
import eu.egm.data.common.SecurityAnalysisResult;
import eu.egm.data.common.ViolationType;
import eu.egm.data.common.WorkflowStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common/lfsa")
public class MockLfsaController {
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
}
