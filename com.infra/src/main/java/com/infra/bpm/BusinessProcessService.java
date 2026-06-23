package com.infra.bpm;

import java.util.Optional;

/**
 * Generic business-process boundary used by service modules to start, manage,
 * and monitor process instances without importing a BPM engine SDK.
 */
public interface BusinessProcessService {
    ProcessInstance start(ProcessStartRequest request);

    void cancel(String processInstanceId);

    ProcessMessageResult correlateMessage(ProcessMessage message);

    Optional<ProcessInstance> findProcessInstance(String processInstanceId);
}
