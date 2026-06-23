package eu.egm.bpm.cgm.importer.delegate;

import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("parseAndTransformDelegate")
public class ParseAndTransformDelegate implements JavaDelegate {
    private final CgmImportServiceClient serviceClient;

    public ParseAndTransformDelegate(CgmImportServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String networkId = (String) execution.getVariable("networkId");
        String objectId = (String) execution.getVariable("objectId");
        try {
            serviceClient.transform(networkId, objectId);
        } catch (RuntimeException exception) {
            serviceClient.updateStatus(networkId, objectId, "Failed", "Failed", List.of(),
                    "BPM parse and transform failed: " + exception.getMessage());
            throw exception;
        }
    }
}
