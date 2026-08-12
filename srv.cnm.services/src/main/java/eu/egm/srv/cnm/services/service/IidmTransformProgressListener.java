package eu.egm.srv.cnm.services.service;

import eu.egm.data.iidm.common.IidmNetworkMergeFailed;
import eu.egm.data.iidm.common.IidmNetworkMergeStatus;
import eu.egm.data.iidm.common.IidmProfileTransformCompleted;
import eu.egm.data.iidm.common.IidmProfileTransformFailed;
import eu.egm.data.iidm.common.IidmProfileTransformStarted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes IIDM lifecycle callbacks and refreshes the CNM import projection.
 */
@Component
public class IidmTransformProgressListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IidmTransformProgressListener.class);

    private final CnmImportRestService importService;

    public IidmTransformProgressListener(CnmImportRestService importService) {
        this.importService = importService;
    }

    @RabbitListener(
            queues = "${cnm.import.event.iidm-transform-started-queue:iidm.profile.transform.started.cnm}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void transformStarted(IidmProfileTransformStarted event) {
        LOGGER.info(
                "Consumed IIDM transform started callback importId={}, fileId={}, transformId={}",
                event.importId(),
                event.fileId(),
                event.transformId());
        importService.updateIidmTransformProgress(event);
    }

    @RabbitListener(
            queues = "${cnm.import.event.iidm-transform-completed-queue:iidm.profile.transform.completed.cnm}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void transformCompleted(IidmProfileTransformCompleted event) {
        LOGGER.info(
                "Consumed IIDM transform completed callback importId={}, fileId={}, transformId={}",
                event.importId(),
                event.fileId(),
                event.transformId());
        importService.updateIidmTransformProgress(event);
    }

    @RabbitListener(
            queues = "${cnm.import.event.iidm-transform-failed-queue:iidm.profile.transform.failed.cnm}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void transformFailed(IidmProfileTransformFailed event) {
        LOGGER.info(
                "Consumed IIDM transform failed callback importId={}, fileId={}, transformId={}",
                event.importId(),
                event.fileId(),
                event.transformId());
        importService.updateIidmTransformProgress(event);
    }

    @RabbitListener(
            queues = "${cnm.import.event.iidm-merge-started-queue:iidm.network.merge.started.cnm}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void mergeStarted(IidmNetworkMergeStatus event) {
        LOGGER.info(
                "Consumed IIDM merge started callback importId={}, networkId={}",
                event.importId(),
                event.mergedNetworkId());
        importService.updateIidmMergeProgress(event);
    }

    @RabbitListener(
            queues = "${cnm.import.event.iidm-merge-completed-queue:iidm.network.merge.completed.cnm}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void mergeCompleted(IidmNetworkMergeStatus event) {
        LOGGER.info(
                "Consumed IIDM merge completed callback importId={}, networkId={}",
                event.importId(),
                event.mergedNetworkId());
        importService.updateIidmMergeProgress(event);
    }

    @RabbitListener(
            queues = "${cnm.import.event.iidm-merge-failed-queue:iidm.network.merge.failed.cnm}",
            containerFactory = "retryingRabbitListenerContainerFactory")
    public void mergeFailed(IidmNetworkMergeFailed event) {
        LOGGER.info(
                "Consumed IIDM merge failed callback importId={}, networkId={}",
                event.importId(),
                event.mergedNetworkId());
        importService.updateIidmMergeProgress(event);
    }
}
