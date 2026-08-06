package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.common.CnmFileProcessingRequested;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer that performs asynchronous RDF metadata extraction for
 * files already persisted in object storage.
 */
@Component
public class CnmFileProcessingListener {
    private final CnmFileProcessingQueue processingQueue;

    public CnmFileProcessingListener(CnmFileProcessingQueue processingQueue) {
        this.processingQueue = processingQueue;
    }

    @RabbitListener(
            queues = "${cnm.import.event.file-processing-queue:cnm.file.process}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void process(CnmFileProcessingRequested event) {
        processingQueue.enqueue(event);
    }
}
