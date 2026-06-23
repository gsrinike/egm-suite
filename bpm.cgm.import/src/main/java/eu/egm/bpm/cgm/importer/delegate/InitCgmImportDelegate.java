package eu.egm.bpm.cgm.importer.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.egm.data.cgm.dto.cgmes.ImportFileStatus;
import eu.egm.data.cgm.dto.cgmes.ImportStatus;
import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("initCgmImportDelegate")
public class InitCgmImportDelegate implements JavaDelegate {
    private final ObjectMapper objectMapper;

    public InitCgmImportDelegate(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ImportStatus status = objectMapper.convertValue(execution.getVariable("importStatus"), ImportStatus.class);
        List<String> objectIds = status.files().stream().map(ImportFileStatus::objectId).toList();
        execution.setVariable("networkId", status.networkId());
        execution.setVariable("objectIds", objectIds);
        execution.setVariable("totalObjectCount", objectIds.size());
    }
}
