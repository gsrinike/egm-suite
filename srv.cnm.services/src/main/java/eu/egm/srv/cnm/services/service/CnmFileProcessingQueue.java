package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.common.CnmFileProcessingRequested;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-process priority queue for RDF metadata extraction. RabbitMQ delivery is
 * intentionally kept light: messages are accepted, prioritized, and processed by
 * bounded workers so boundary profiles are available before model groups are
 * evaluated for IIDM transformation.
 */
@Component
public class CnmFileProcessingQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(CnmFileProcessingQueue.class);
    private final PriorityBlockingQueue<CnmFileProcessingPriority.PrioritizedRequest> queue =
            new PriorityBlockingQueue<>(64, CnmFileProcessingPriority.COMPARATOR);
    private final CnmImportRestService importService;
    private final int workerCount;
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running;

    public CnmFileProcessingQueue(
            CnmImportRestService importService,
            @Value("${cnm.import.metadata.worker-count:1}") int workerCount) {
        this.importService = importService;
        this.workerCount = Math.max(workerCount, 1);
    }

    public void enqueue(CnmFileProcessingRequested event) {
        if (event == null) {
            return;
        }
        queue.offer(CnmFileProcessingPriority.prioritize(event));
    }

    @PostConstruct
    void start() {
        running = true;
        for (int index = 0; index < workerCount; index++) {
            Thread worker = new Thread(this::runWorker, "cnm-rdf-priority-worker-" + index);
            worker.setDaemon(true);
            worker.start();
            workers.add(worker);
        }
    }

    @PreDestroy
    void stop() {
        running = false;
        workers.forEach(Thread::interrupt);
    }

    private void runWorker() {
        while (running) {
            try {
                importService.processFile(queue.take().event());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception) {
                LOGGER.warn("Unable to process queued CNM RDF metadata request", exception);
            }
        }
    }
}
