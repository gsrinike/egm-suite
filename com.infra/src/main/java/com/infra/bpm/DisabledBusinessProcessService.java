package com.infra.bpm;

import java.util.Optional;

/**
 * Default BPM adapter used when no BPM engine is configured for a service.
 */
public class DisabledBusinessProcessService implements BusinessProcessService {
    @Override
    public ProcessInstance start(ProcessStartRequest request) {
        throw disabled();
    }

    @Override
    public void cancel(String processInstanceId) {
        throw disabled();
    }

    @Override
    public ProcessMessageResult correlateMessage(ProcessMessage message) {
        throw disabled();
    }

    @Override
    public Optional<ProcessInstance> findProcessInstance(String processInstanceId) {
        return Optional.empty();
    }

    private IllegalStateException disabled() {
        return new IllegalStateException("BPM capability is not enabled. Set utility.bpm.camunda.enabled=true.");
    }
}
