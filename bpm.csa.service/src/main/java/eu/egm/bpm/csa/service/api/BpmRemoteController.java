package eu.egm.bpm.csa.service.api;

import com.infra.bpm.BusinessProcessService;
import com.infra.bpm.ProcessInstance;
import com.infra.bpm.ProcessMessage;
import com.infra.bpm.ProcessMessageResult;
import com.infra.bpm.ProcessStartRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bpm")
public class BpmRemoteController {
    private final BusinessProcessService businessProcessService;

    public BpmRemoteController(BusinessProcessService businessProcessService) {
        this.businessProcessService = businessProcessService;
    }

    @PostMapping(value = "/processes/{processId}/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProcessInstance start(@PathVariable String processId, @RequestBody ProcessStartRequest request) {
        return businessProcessService.start(new ProcessStartRequest(processId, request.variables(), request.businessKey()));
    }

    @PostMapping(value = "/instances/{processInstanceId}/cancel")
    public void cancel(@PathVariable String processInstanceId) {
        businessProcessService.cancel(processInstanceId);
    }

    @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProcessMessageResult correlate(@RequestBody ProcessMessage message) {
        return businessProcessService.correlateMessage(message);
    }

    @GetMapping("/instances/{processInstanceId}")
    public ProcessInstance get(@PathVariable String processInstanceId) {
        return businessProcessService.findProcessInstance(processInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Process instance not found: " + processInstanceId));
    }
}
