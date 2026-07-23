package eu.egm.srv.iidm.transformer.service;

import eu.egm.data.iidm.common.IidmProfileTransformRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes IIDM transform requests emitted by CNM profile processing.
 */
@Component
public class IidmProfileTransformListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IidmProfileTransformListener.class);

    private final IidmProfileTransformService transformService;

    public IidmProfileTransformListener(IidmProfileTransformService transformService) {
        this.transformService = transformService;
    }

    @RabbitListener(
            queues = "${iidm.transform.event.requested-queue:iidm.profile.transform}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void transform(IidmProfileTransformRequested request) {
        LOGGER.info(
                "Consumed IIDM transform request importId={}, fileId={}, sourceFiles={}",
                request.importId(),
                request.fileId(),
                request.sourceFiles() == null ? 0 : request.sourceFiles().size());
        transformService.transform(request);
    }
}
