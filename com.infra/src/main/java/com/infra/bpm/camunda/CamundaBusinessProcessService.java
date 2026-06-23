package com.infra.bpm.camunda;

import com.infra.bpm.BusinessProcessService;
import com.infra.bpm.ProcessInstance;
import com.infra.bpm.ProcessInstanceStatus;
import com.infra.bpm.ProcessMessage;
import com.infra.bpm.ProcessMessageResult;
import com.infra.bpm.ProcessStartRequest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;

/**
 * Embedded Camunda Platform adapter. BPMN resources and delegates are supplied
 * by consuming applications or bpm.* modules; com.infra owns engine invocation.
 */
public class CamundaBusinessProcessService implements BusinessProcessService {
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;

    public CamundaBusinessProcessService(RuntimeService runtimeService,
                                         RepositoryService repositoryService,
                                         HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
    }

    @Override
    public ProcessInstance start(ProcessStartRequest request) {
        org.camunda.bpm.engine.runtime.ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                request.processId(),
                request.businessKey(),
                request.variables());
        return toProcessInstance(instance);
    }

    @Override
    public void cancel(String processInstanceId) {
        runtimeService.deleteProcessInstance(processInstanceId, "Canceled by infrastructure BPM adapter");
    }

    @Override
    public ProcessMessageResult correlateMessage(ProcessMessage message) {
        org.camunda.bpm.engine.runtime.ProcessInstance instance = runtimeService.createMessageCorrelation(message.name())
                .processInstanceBusinessKey(message.correlationKey())
                .setVariables(message.variables())
                .correlateWithResult()
                .getProcessInstance();
        return new ProcessMessageResult(instance == null ? null : instance.getProcessInstanceId());
    }

    @Override
    public Optional<ProcessInstance> findProcessInstance(String processInstanceId) {
        org.camunda.bpm.engine.runtime.ProcessInstance activeInstance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (activeInstance != null) {
            return Optional.of(toProcessInstance(activeInstance));
        }

        HistoricProcessInstance historicInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        return Optional.ofNullable(historicInstance).map(this::toProcessInstance);
    }

    private ProcessInstance toProcessInstance(org.camunda.bpm.engine.runtime.ProcessInstance instance) {
        ProcessDefinition definition = processDefinition(instance.getProcessDefinitionId());
        return new ProcessInstance(
                instance.getProcessInstanceId(),
                definition == null ? null : definition.getKey(),
                definition == null ? 0 : definition.getVersion(),
                status(instance.getProcessInstanceId()),
                null,
                null);
    }

    private ProcessInstance toProcessInstance(HistoricProcessInstance instance) {
        ProcessDefinition definition = processDefinition(instance.getProcessDefinitionId());
        return new ProcessInstance(
                instance.getId(),
                definition == null ? null : definition.getKey(),
                definition == null ? 0 : definition.getVersion(),
                historicStatus(instance),
                toOffsetDateTime(instance.getStartTime()).orElse(null),
                toOffsetDateTime(instance.getEndTime()).orElse(null));
    }

    private ProcessDefinition processDefinition(String processDefinitionId) {
        if (processDefinitionId == null || processDefinitionId.isBlank()) {
            return null;
        }
        return repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
    }

    private ProcessInstanceStatus status(String processInstanceId) {
        long incidentCount = runtimeService
                .createIncidentQuery()
                .processInstanceId(processInstanceId)
                .count();
        return incidentCount > 0 ? ProcessInstanceStatus.INCIDENT : ProcessInstanceStatus.ACTIVE;
    }

    private ProcessInstanceStatus historicStatus(HistoricProcessInstance instance) {
        if (instance.getEndTime() == null) {
            return ProcessInstanceStatus.ACTIVE;
        }
        String deleteReason = instance.getDeleteReason();
        return deleteReason == null || deleteReason.isBlank()
                ? ProcessInstanceStatus.COMPLETED
                : ProcessInstanceStatus.CANCELED;
    }

    private Optional<OffsetDateTime> toOffsetDateTime(Date date) {
        return Optional.ofNullable(date)
                .map(value -> value.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
}
