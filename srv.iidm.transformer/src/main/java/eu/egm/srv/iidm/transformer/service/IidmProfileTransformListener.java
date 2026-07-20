package eu.egm.srv.iidm.transformer.service;

import eu.egm.data.iidm.common.IidmProfileTransformRequested;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes IIDM transform requests emitted by CNM profile processing.
 */
@Component
public class IidmProfileTransformListener {
    private final IidmProfileTransformService transformService;

    public IidmProfileTransformListener(IidmProfileTransformService transformService) {
        this.transformService = transformService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${iidm.transform.event.requested-queue:iidm.profile.transform}", durable = "true"),
            exchange = @Exchange(value = "${iidm.transform.event.exchange:iidm.events}", type = "topic", durable = "true"),
            key = "${iidm.transform.event.requested-routing-key:iidm.profile.transform.requested}"))
    public void transform(IidmProfileTransformRequested request) {
        transformService.transform(request);
    }
}
