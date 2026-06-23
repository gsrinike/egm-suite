package eu.egm.bpm.cgm.importer.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("updateImportStatusDelegate")
public class UpdateImportStatusDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("statusUpdateCompleted", true);
    }
}
