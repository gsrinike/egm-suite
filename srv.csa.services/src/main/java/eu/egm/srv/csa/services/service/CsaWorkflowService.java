package eu.egm.srv.csa.services.service;

import com.infra.bpm.BusinessProcessService;
import com.infra.bpm.ProcessStartRequest;
import com.utils.restservice.RestServiceSupport;
import eu.egm.data.common.CommonPage;
import eu.egm.data.common.CsaCaseStatus;
import eu.egm.data.common.CsaStartRequest;
import eu.egm.data.common.LoadFlowRequest;
import eu.egm.data.common.LoadFlowResult;
import eu.egm.data.common.RaoRequest;
import eu.egm.data.common.RaoResult;
import eu.egm.data.common.SecurityAnalysisRequest;
import eu.egm.data.common.SecurityAnalysisResult;
import eu.egm.data.common.WorkflowStatus;
import eu.egm.data.common.WorkflowTaskStatus;
import eu.egm.data.common.WorkflowTaskView;
import io.micrometer.observation.ObservationRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CsaWorkflowService extends RestServiceSupport {
    private static final String CSA_PROCESS_ID = "csa-end-to-end";

    private final RestTemplate restTemplate;
    private final BusinessProcessService businessProcessService;
    private final String lfsaBaseUrl;
    private final String raoBaseUrl;
    private final Map<String, CsaCaseStatus> cases = new ConcurrentHashMap<>();

    public CsaWorkflowService(
            Environment environment,
            ObservationRegistry observationRegistry,
            RestTemplate restTemplate,
            BusinessProcessService businessProcessService,
            @Value("${csa.services.lfsa-base-url:http://localhost:8091}") String lfsaBaseUrl,
            @Value("${csa.services.rao-base-url:http://localhost:8093}") String raoBaseUrl) {
        super(environment, observationRegistry);
        this.restTemplate = restTemplate;
        this.businessProcessService = businessProcessService;
        this.lfsaBaseUrl = trim(lfsaBaseUrl);
        this.raoBaseUrl = trim(raoBaseUrl);
    }

    public CsaCaseStatus start(CsaStartRequest request) {
        String caseId = "csa-" + UUID.randomUUID();
        Instant now = Instant.now();
        String processInstanceId = startBpm(caseId, request);
        List<WorkflowTaskView> tasks = new ArrayList<>();
        tasks.add(task("init", "Initialize CSA case", WorkflowTaskStatus.COMPLETED, now, "CSA case accepted"));

        LoadFlowResult loadFlow = runLoadFlow(caseId, request);
        tasks.add(task("lf", "Run Load Flow", WorkflowTaskStatus.COMPLETED, Instant.now(), loadFlow.message()));

        SecurityAnalysisResult securityAnalysis = runSecurityAnalysis(caseId, request, loadFlow);
        tasks.add(task("sa", "Run Security Analysis", WorkflowTaskStatus.COMPLETED, Instant.now(), securityAnalysis.message()));

        RaoResult rao = null;
        if (request.optimizeRemedialActions()) {
            rao = runRao(caseId, request, securityAnalysis);
            tasks.add(task("rao", "Optimize Remedial Actions", WorkflowTaskStatus.COMPLETED, Instant.now(), rao.message()));
        } else {
            tasks.add(task("rao", "Optimize Remedial Actions", WorkflowTaskStatus.SKIPPED, Instant.now(), "RAO disabled for this CSA case"));
        }

        CsaCaseStatus status = new CsaCaseStatus(
                caseId,
                request.caseName(),
                WorkflowStatus.COMPLETED,
                request.networkCase(),
                processInstanceId,
                loadFlow,
                securityAnalysis,
                rao,
                tasks,
                now,
                Instant.now(),
                "CSA workflow completed");
        cases.put(caseId, status);
        return status;
    }

    public CommonPage<CsaCaseStatus> list(int page, int size) {
        List<CsaCaseStatus> sorted = cases.values().stream()
                .sorted(Comparator.comparing(CsaCaseStatus::createdAt).reversed())
                .toList();
        int from = Math.min(Math.max(page, 0) * Math.max(size, 1), sorted.size());
        int to = Math.min(from + Math.max(size, 1), sorted.size());
        return new CommonPage<>(sorted.subList(from, to), sorted.size(), page, size);
    }

    public CsaCaseStatus get(String csaCaseId) {
        CsaCaseStatus status = cases.get(csaCaseId);
        if (status == null) {
            throw new IllegalArgumentException("CSA case not found: " + csaCaseId);
        }
        return status;
    }

    private String startBpm(String caseId, CsaStartRequest request) {
        try {
            return businessProcessService.start(new ProcessStartRequest(
                    CSA_PROCESS_ID,
                    Map.of("csaCaseId", caseId, "caseName", request.caseName()),
                    caseId)).processInstanceId();
        } catch (RuntimeException exception) {
            logger.warn("CSA BPM process could not be started; using local workflow id: {}", exception.getMessage());
            return "local-" + caseId;
        }
    }

    private LoadFlowResult runLoadFlow(String caseId, CsaStartRequest request) {
        LoadFlowRequest lfRequest = new LoadFlowRequest(caseId, request.networkCase(), true, true, "Warm Start");
        try {
            return Objects.requireNonNull(
                    restTemplate.postForObject(lfsaBaseUrl + "/api/common/lfsa/load-flow", lfRequest, LoadFlowResult.class),
                    "LF/SA load-flow service returned no response body");
        } catch (RestClientException exception) {
            throw new IllegalStateException("Unable to run LF/SA load-flow service", exception);
        }
    }

    private SecurityAnalysisResult runSecurityAnalysis(String caseId, CsaStartRequest request, LoadFlowResult loadFlow) {
        SecurityAnalysisRequest saRequest = new SecurityAnalysisRequest(caseId, request.networkCase(), request.contingencyIds(), loadFlow);
        try {
            return Objects.requireNonNull(
                    restTemplate.postForObject(lfsaBaseUrl + "/api/common/lfsa/security-analysis", saRequest, SecurityAnalysisResult.class),
                    "LF/SA security-analysis service returned no response body");
        } catch (RestClientException exception) {
            throw new IllegalStateException("Unable to run LF/SA security-analysis service", exception);
        }
    }

    private RaoResult runRao(String caseId, CsaStartRequest request, SecurityAnalysisResult securityAnalysis) {
        RaoRequest raoRequest = new RaoRequest(caseId, request.networkCase(), securityAnalysis, 100.0, 4, 6);
        try {
            return Objects.requireNonNull(
                    restTemplate.postForObject(raoBaseUrl + "/api/common/rao/optimize", raoRequest, RaoResult.class),
                    "RAO service returned no response body");
        } catch (RestClientException exception) {
            throw new IllegalStateException("Unable to run RAO service", exception);
        }
    }

    private WorkflowTaskView task(String id, String name, WorkflowTaskStatus status, Instant at, String message) {
        return new WorkflowTaskView(id, name, status, at, status == WorkflowTaskStatus.RUNNING ? null : at, message);
    }

    private String trim(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
