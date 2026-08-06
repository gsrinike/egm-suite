package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.common.CnmTransformInitializationRequested;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Starts asynchronous RDF processing after all raw import files are safely
 * stored in object storage.
 */
@Component
public class CnmTransformInitializationListener {
    private final CnmImportRestService importService;

    public CnmTransformInitializationListener(CnmImportRestService importService) {
        this.importService = importService;
    }

    @RabbitListener(
            queues = "${cnm.import.event.transform-initialization-queue:cnm.transform.initialize}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void initialize(CnmTransformInitializationRequested event) {
        importService.initializeTransform(event);
    }
}
