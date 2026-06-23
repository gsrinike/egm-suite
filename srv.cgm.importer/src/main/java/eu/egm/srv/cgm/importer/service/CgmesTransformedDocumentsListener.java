package eu.egm.srv.cgm.importer.service;

import eu.egm.srv.cgm.importer.config.CgmImportMessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CgmesTransformedDocumentsListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmesTransformedDocumentsListener.class);

    private final IidmConversionService iidmConversionService;
    private final CgmImportService importService;

    public CgmesTransformedDocumentsListener(IidmConversionService iidmConversionService,
                                             CgmImportService importService) {
        this.iidmConversionService = iidmConversionService;
        this.importService = importService;
    }

    @RabbitListener(queues = CgmImportMessagingConfig.IIDM_TRANSFORM_QUEUE)
    public void onCgmesTransformed(CgmesTransformedDocumentsEvent event) {
        try {
            iidmConversionService.convert(event.networkId());
            importService.updateFileStatus(event.networkId(), event.objectId(), CgmImportService.STATE_COMPLETE,
                    "Done", event.documentIds(), "IIDM transform completed");
        } catch (RuntimeException exception) {
            LOGGER.warn("IIDM transform failed for network {} object {}", event.networkId(), event.objectId(), exception);
            importService.updateFileStatus(event.networkId(), event.objectId(), CgmImportService.STATE_COMPLETE,
                    CgmImportService.STATE_FAILED, event.documentIds(), "IIDM transform failed: " + exception.getMessage());
        }
    }
}
