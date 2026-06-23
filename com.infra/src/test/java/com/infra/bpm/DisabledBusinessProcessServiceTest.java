package com.infra.bpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisabledBusinessProcessServiceTest {
    @Test
    void returnsEmptyMonitorResultWhenBpmIsDisabled() {
        DisabledBusinessProcessService service = new DisabledBusinessProcessService();

        assertThat(service.findProcessInstance("42")).isEmpty();
    }

    @Test
    void rejectsMutationWhenBpmIsDisabled() {
        DisabledBusinessProcessService service = new DisabledBusinessProcessService();

        assertThatThrownBy(() -> service.start(new ProcessStartRequest("import-process", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("utility.bpm.camunda.enabled=true");
    }
}
