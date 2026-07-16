package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.common.CnmFileProcessingRequested;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer that performs asynchronous RDF metadata extraction for
 * files already persisted in object storage.
 */
@Component
public class CnmFileProcessingListener {
    private final CnmImportRestService importService;

    public CnmFileProcessingListener(CnmImportRestService importService) {
        this.importService = importService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${cnm.import.event.file-processing-queue:cnm.file.process}", durable = "true"),
            exchange = @Exchange(value = "${cnm.import.event.exchange:cnm.events}", type = "topic", durable = "true"),
            key = "${cnm.import.event.file-processing-routing-key:cnm.file.processing.requested}"))
    public void process(CnmFileProcessingRequested event) {
        importService.processFile(event);
    }
}
