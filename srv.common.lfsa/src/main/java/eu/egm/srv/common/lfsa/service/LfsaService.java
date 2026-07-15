package eu.egm.srv.common.lfsa.service;

import com.utils.restservice.RestServiceSupport;
import eu.egm.data.common.ContingencyViolation;
import eu.egm.data.common.LineFlow;
import eu.egm.data.common.LoadFlowRequest;
import eu.egm.data.common.LoadFlowResult;
import eu.egm.data.common.SecurityAnalysisRequest;
import eu.egm.data.common.SecurityAnalysisResult;
import eu.egm.data.common.ViolationType;
import eu.egm.data.common.WorkflowStatus;
import io.micrometer.observation.ObservationRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class LfsaService extends RestServiceSupport {
    public LfsaService(Environment environment, ObservationRegistry observationRegistry) {
        super(environment, observationRegistry);
    }

    public LoadFlowResult runLoadFlow(LoadFlowRequest request) {
        logger.info("{} running load flow for {}", moduleName(), request.networkCase());
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
        logger.info("{} running security analysis for {}", moduleName(), request.networkCase());
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

    private String id(String requestId, String prefix) {
        return requestId == null || requestId.isBlank() ? prefix + "-" + UUID.randomUUID() : requestId + "-" + prefix;
    }

    private String contingency(SecurityAnalysisRequest request, int index) {
        return request.contingencyIds().size() > index ? request.contingencyIds().get(index) : "N-1-" + (index + 1);
    }
}
