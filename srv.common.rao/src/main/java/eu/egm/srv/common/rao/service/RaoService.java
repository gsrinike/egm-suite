package eu.egm.srv.common.rao.service;

import com.utils.restservice.RestServiceSupport;
import eu.egm.data.common.RaoRequest;
import eu.egm.data.common.RaoResult;
import eu.egm.data.common.RemedialAction;
import eu.egm.data.common.WorkflowStatus;
import io.micrometer.observation.ObservationRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class RaoService extends RestServiceSupport {
    public RaoService(Environment environment, ObservationRegistry observationRegistry) {
        super(environment, observationRegistry);
    }

    public RaoResult optimize(RaoRequest request) {
        logger.info("{} optimizing remedial actions for {}", moduleName(), request.networkCase());
        List<RemedialAction> actions = List.of(
                new RemedialAction("RA-1", "PST-BE-NL-1", "PST_TAP", "-1", "1", 84.0, "VALIDATED"),
                new RemedialAction("RA-2", "GEN-DE-4", "REDISPATCH", "255 MW", "218 MW", 37.0, "VALIDATED"));
        return new RaoResult(
                request.requestId() == null || request.requestId().isBlank() ? "rao-" + UUID.randomUUID() : request.requestId() + "-rao",
                WorkflowStatus.COMPLETED,
                actions,
                maxObserved(request),
                Math.max(90.0, maxObserved(request) - 18.0),
                Math.max(94.0, maxObserved(request) - 15.0),
                Instant.now(),
                "RAO completed with deterministic remedial actions");
    }

    private double maxObserved(RaoRequest request) {
        if (request.securityAnalysisResult() == null) {
            return request.loadingThreshold() + 20.0;
        }
        return request.securityAnalysisResult().postContingencyViolations().stream()
                .mapToDouble(violation -> violation.observedValue())
                .max()
                .orElse(request.loadingThreshold() + 20.0);
    }
}
