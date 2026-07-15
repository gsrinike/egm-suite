package eu.egm.srv.common.rao.api;

import eu.egm.data.common.RaoRequest;
import eu.egm.data.common.RaoResult;
import eu.egm.srv.common.rao.service.RaoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common/rao")
public class RaoController {
    private final RaoService service;

    public RaoController(RaoService service) {
        this.service = service;
    }

    @PostMapping(value = "/optimize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RaoResult optimize(@RequestBody RaoRequest request) {
        return service.optimize(request);
    }
}
