package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.common.CnmSnapshotAssemblyRequested;
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

    @RabbitListener(
            queues = "${cnm.import.event.snapshot-assembly-queue:cnm.snapshot.assemble}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void assemble(CnmSnapshotAssemblyRequested event) {
        importService.assembleSnapshot(event);
    }
}
