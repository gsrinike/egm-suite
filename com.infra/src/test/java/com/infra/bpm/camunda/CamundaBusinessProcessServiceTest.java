package com.infra.bpm.camunda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.infra.bpm.ProcessInstanceStatus;
import com.infra.bpm.ProcessStartRequest;
import java.util.Map;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.IncidentQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;

class CamundaBusinessProcessServiceTest {
    @Test
    void startsProcessThroughEmbeddedRuntimeService() {
        RuntimeService runtimeService = mock(RuntimeService.class);
        RepositoryService repositoryService = mock(RepositoryService.class);
        HistoryService historyService = mock(HistoryService.class);
        ProcessInstance camundaInstance = mock(ProcessInstance.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        ProcessDefinitionQuery definitionQuery = mock(ProcessDefinitionQuery.class);
        IncidentQuery incidentQuery = mock(IncidentQuery.class);

        when(runtimeService.startProcessInstanceByKey("sample-process", "business-1", Map.of("businessKey", "business-1")))
                .thenReturn(camundaInstance);
        when(camundaInstance.getProcessInstanceId()).thenReturn("pi-1");
        when(camundaInstance.getProcessDefinitionId()).thenReturn("definition-1");
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(definitionQuery);
        when(definitionQuery.processDefinitionId("definition-1")).thenReturn(definitionQuery);
        when(definitionQuery.singleResult()).thenReturn(definition);
        when(definition.getKey()).thenReturn("sample-process");
        when(definition.getVersion()).thenReturn(3);
        when(runtimeService.createIncidentQuery()).thenReturn(incidentQuery);
        when(incidentQuery.processInstanceId("pi-1")).thenReturn(incidentQuery);
        when(incidentQuery.count()).thenReturn(0L);

        CamundaBusinessProcessService service = new CamundaBusinessProcessService(
                runtimeService,
                repositoryService,
                historyService);

        assertThat(service.start(new ProcessStartRequest("sample-process", Map.of("businessKey", "business-1"), "business-1")))
                .satisfies(instance -> {
                    assertThat(instance.processInstanceId()).isEqualTo("pi-1");
                    assertThat(instance.processId()).isEqualTo("sample-process");
                    assertThat(instance.version()).isEqualTo(3);
                    assertThat(instance.status()).isEqualTo(ProcessInstanceStatus.ACTIVE);
                });
    }

    @Test
    void findsNoProcessWhenRuntimeAndHistoryAreEmpty() {
        RuntimeService runtimeService = mock(RuntimeService.class);
        RepositoryService repositoryService = mock(RepositoryService.class);
        HistoryService historyService = mock(HistoryService.class);
        org.camunda.bpm.engine.runtime.ProcessInstanceQuery runtimeQuery =
                mock(org.camunda.bpm.engine.runtime.ProcessInstanceQuery.class);
        org.camunda.bpm.engine.history.HistoricProcessInstanceQuery historyQuery =
                mock(org.camunda.bpm.engine.history.HistoricProcessInstanceQuery.class);

        when(runtimeService.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("missing")).thenReturn(runtimeQuery);
        when(runtimeQuery.singleResult()).thenReturn(null);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historyQuery);
        when(historyQuery.processInstanceId("missing")).thenReturn(historyQuery);
        when(historyQuery.singleResult()).thenReturn(null);

        CamundaBusinessProcessService service = new CamundaBusinessProcessService(
                runtimeService,
                repositoryService,
                historyService);

        assertThat(service.findProcessInstance("missing")).isEmpty();
    }
}
