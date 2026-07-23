package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.common.CnmSnapshotAssemblyRequested;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer that assembles stitched CGM network snapshots separately
 * from RDF profile parsing.
 */
@Component
public class CnmSnapshotAssemblyListener {
    private final CnmImportRestService importService;

    public CnmSnapshotAssemblyListener(CnmImportRestService importService) {
        this.importService = importService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${cnm.import.event.snapshot-assembly-queue:cnm.snapshot.assemble}", durable = "true"),
            exchange = @Exchange(value = "${cnm.import.event.exchange:cnm.events}", type = "topic", durable = "true"),
            key = "${cnm.import.event.snapshot-assembly-routing-key:cnm.snapshot.assembly.requested}"))
    public void assemble(CnmSnapshotAssemblyRequested event) {
        importService.assembleSnapshot(event);
    }
}
