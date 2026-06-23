package com.infra.config;

import com.infra.bpm.BusinessProcessService;
import com.infra.bpm.camunda.CamundaBusinessProcessService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(RuntimeService.class)
public class CamundaBpmInfrastructureConfig {
    @Bean
    @ConditionalOnMissingBean(BusinessProcessService.class)
    BusinessProcessService camundaBusinessProcessService(
            RuntimeService runtimeService,
            RepositoryService repositoryService,
            HistoryService historyService) {
        return new CamundaBusinessProcessService(runtimeService, repositoryService, historyService);
    }
}
