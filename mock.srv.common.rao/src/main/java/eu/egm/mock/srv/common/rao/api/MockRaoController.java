package eu.egm.mock.srv.common.rao.api;

import eu.egm.data.common.RaoRequest;
import eu.egm.data.common.RaoResult;
import eu.egm.data.common.RemedialAction;
import eu.egm.data.common.WorkflowStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common/rao")
public class MockRaoController {
    @PostMapping(value = "/optimize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RaoResult optimize(@RequestBody RaoRequest request) {
        return new RaoResult(
                "mock-rao-" + request.requestId(),
                WorkflowStatus.COMPLETED,
                List.of(new RemedialAction("MOCK-RA-1", "MOCK-PST-1", "PST_TAP", "0", "2", 58.0, "VALIDATED")),
                118.0,
                99.0,
                101.0,
                Instant.now(),
                "Mock RAO completed");
    }
}
