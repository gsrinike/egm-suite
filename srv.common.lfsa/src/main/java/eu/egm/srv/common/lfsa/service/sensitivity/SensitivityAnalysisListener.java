package eu.egm.srv.common.lfsa.service.sensitivity;

import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRequested;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes asynchronous sensitivity-analysis requests.
 */
@Component
public class SensitivityAnalysisListener {
    private final SensitivityAnalysisService service;

    public SensitivityAnalysisListener(SensitivityAnalysisService service) {
        this.service = service;
    }

    @RabbitListener(
            queues = "${lfsa.sensitivity.event.requested-queue:lfsa.sensitivity}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void onSensitivityAnalysisRequested(SensitivityAnalysisRequested event) {
        service.process(event);
    }
}
