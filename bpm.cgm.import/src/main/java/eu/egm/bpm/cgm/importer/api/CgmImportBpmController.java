package eu.egm.bpm.cgm.importer.api;

import com.infra.InfrastructureUtils;
import com.infra.bpm.ProcessInstance;
import com.infra.bpm.ProcessMessage;
import com.infra.bpm.ProcessMessageResult;
import com.infra.bpm.ProcessStartRequest;
import eu.egm.data.cgm.dto.cgmes.ImportFileStatus;
import eu.egm.data.cgm.dto.cgmes.ImportStatus;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bpm/cgm-imports")
public class CgmImportBpmController {
    private static final String PROCESS_ID = "cgm-import";

    private final InfrastructureUtils infrastructureUtils;

    public CgmImportBpmController(InfrastructureUtils infrastructureUtils) {
        this.infrastructureUtils = infrastructureUtils;
    }

    @PostMapping("/start")
    @Operation(summary = "Start the CGM import BPM process")
    public ProcessInstance start(@RequestBody ImportStatus status) {
        return infrastructureUtils.businessProcessService().start(new ProcessStartRequest(
                PROCESS_ID,
                Map.of(
                        "networkId", status.networkId(),
                        "importStatus", status,
                        "objectIds", status.files().stream().map(ImportFileStatus::objectId).toList()),
                status.networkId()));
    }

    @PostMapping("/callback")
    @Operation(summary = "Correlate an external callback message into the CGM import BPM process")
    public ProcessMessageResult callback(@RequestBody CgmImportCallback callback) {
        return infrastructureUtils.businessProcessService().correlateMessage(new ProcessMessage(
                callback.messageName(),
                callback.correlationKey(),
                callback.variables(),
                null));
    }

    @GetMapping("/instances/{processInstanceId}")
    @Operation(summary = "Find a CGM import process instance")
    public ProcessInstance instance(@PathVariable String processInstanceId) {
        return infrastructureUtils.businessProcessService()
                .findProcessInstance(processInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown process instance " + processInstanceId));
    }

    @PostMapping("/instances/{processInstanceId}/cancel")
    @Operation(summary = "Cancel a CGM import process instance")
    public void cancel(@PathVariable String processInstanceId) {
        infrastructureUtils.businessProcessService().cancel(processInstanceId);
    }

    public record CgmImportCallback(String messageName, String correlationKey, Map<String, Object> variables) {
    }
}
