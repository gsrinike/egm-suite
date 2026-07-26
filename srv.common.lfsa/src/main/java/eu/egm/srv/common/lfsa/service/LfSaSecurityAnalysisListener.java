package eu.egm.srv.common.lfsa.service;

import eu.egm.data.common.SecurityAnalysisRequested;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes asynchronous security-analysis requests.
 */
@Component
public class LfSaSecurityAnalysisListener {
    private final LfSaService service;

    public LfSaSecurityAnalysisListener(LfSaService service) {
        this.service = service;
    }

    @RabbitListener(
            queues = "${lfsa.security-analysis.event.requested-queue:lfsa.security-analysis}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void onSecurityAnalysisRequested(SecurityAnalysisRequested event) {
        service.processSecurityAnalysis(event);
    }
}
