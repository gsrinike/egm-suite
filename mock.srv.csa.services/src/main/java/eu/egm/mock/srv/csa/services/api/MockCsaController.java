package eu.egm.mock.srv.csa.services.api;

import eu.egm.data.common.CommonPage;
import eu.egm.data.common.ContingencyViolation;
import eu.egm.data.common.CsaCaseStatus;
import eu.egm.data.common.CsaStartRequest;
import eu.egm.data.common.LineFlow;
import eu.egm.data.common.LoadFlowResult;
import eu.egm.data.common.RaoResult;
import eu.egm.data.common.RemedialAction;
import eu.egm.data.common.SecurityAnalysisResult;
import eu.egm.data.common.ViolationType;
import eu.egm.data.common.WorkflowStatus;
import eu.egm.data.common.WorkflowTaskStatus;
import eu.egm.data.common.WorkflowTaskView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/csa/cases")
public class MockCsaController {
    private final List<CsaCaseStatus> cases = new ArrayList<>();

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CsaCaseStatus start(@RequestBody CsaStartRequest request) {
        CsaCaseStatus status = sample(request);
        cases.add(0, status);
        return status;
    }

    @GetMapping
    public CommonPage<CsaCaseStatus> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        int from = Math.min(page * size, cases.size());
        int to = Math.min(from + size, cases.size());
        return new CommonPage<>(cases.subList(from, to), cases.size(), page, size);
    }

    @GetMapping("/{csaCaseId}")
    public CsaCaseStatus get(@PathVariable String csaCaseId) {
        return cases.stream()
                .filter(status -> status.csaCaseId().equals(csaCaseId))
                .findFirst()
                .orElseGet(() -> sample(new CsaStartRequest("Mock CSA", null, List.of("N-1-LINE"), true)));
    }

    private CsaCaseStatus sample(CsaStartRequest request) {
        Instant now = Instant.now();
        String id = "mock-csa-" + UUID.randomUUID();
        LoadFlowResult lf = new LoadFlowResult(
                id + "-lf",
                WorkflowStatus.COMPLETED,
                List.of(new LineFlow("MOCK-LINE-1", "BUS-A", "BUS-B", 180.0, 21.0, 82.0)),
                now,
                "Mock LF completed");
        SecurityAnalysisResult sa = new SecurityAnalysisResult(
                id + "-sa",
                WorkflowStatus.COMPLETED,
                List.of(),
                List.of(new ContingencyViolation("N-1-LINE", "MOCK-LINE-2", ViolationType.OVERLOAD, 116.0, 100.0, "%", "HIGH")),
                now,
                "Mock SA completed");
        RaoResult rao = new RaoResult(
                id + "-rao",
                WorkflowStatus.COMPLETED,
                List.of(new RemedialAction("MOCK-RA-1", "MOCK-PST", "PST_TAP", "0", "1", 44.0, "VALIDATED")),
                116.0,
                98.0,
                100.0,
                now,
                "Mock RAO completed");
        return new CsaCaseStatus(
                id,
                request.caseName(),
                WorkflowStatus.COMPLETED,
                request.networkCase(),
                "mock-process-" + id,
                lf,
                sa,
                rao,
                List.of(
                        new WorkflowTaskView("init", "Initialize CSA case", WorkflowTaskStatus.COMPLETED, now, now, "Mock init"),
                        new WorkflowTaskView("lfsa", "Run LF/SA", WorkflowTaskStatus.COMPLETED, now, now, "Mock LF/SA"),
                        new WorkflowTaskView("rao", "Run RAO", WorkflowTaskStatus.COMPLETED, now, now, "Mock RAO")),
                now,
                now,
                "Mock CSA workflow completed");
    }
}
