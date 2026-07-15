package eu.egm.srv.csa.services.api;

import eu.egm.data.common.CommonPage;
import eu.egm.data.common.CsaCaseStatus;
import eu.egm.data.common.CsaStartRequest;
import eu.egm.srv.csa.services.service.CsaWorkflowService;
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
public class CsaController {
    private final CsaWorkflowService service;

    public CsaController(CsaWorkflowService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CsaCaseStatus start(@RequestBody CsaStartRequest request) {
        return service.start(request);
    }

    @GetMapping
    public CommonPage<CsaCaseStatus> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.list(page, size);
    }

    @GetMapping("/{csaCaseId}")
    public CsaCaseStatus get(@PathVariable String csaCaseId) {
        return service.get(csaCaseId);
    }
}
