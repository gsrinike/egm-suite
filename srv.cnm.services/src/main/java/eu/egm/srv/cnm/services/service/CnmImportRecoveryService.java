package eu.egm.srv.cnm.services.service;

import com.infra.InfrastructureUtils;
import com.infra.event.EventPublisherService;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSort;
import eu.egm.data.cnm.cgmes.CgmesProfileKind;
import eu.egm.data.cnm.common.CnmFileProcessingRequested;
import eu.egm.data.cnm.common.CnmTransformInitializationRequested;
import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.srv.cnm.services.domain.CnmImportDocument;
import eu.egm.srv.cnm.services.domain.CnmImportDocument.CnmImportFileDocument;
import eu.egm.srv.cnm.services.domain.CnmImportDocumentAdapter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Periodically requeues import work that was persisted but never acknowledged by
 * the asynchronous consumers. Keeping this outside read APIs makes import
 * lookups deterministic while still protecting long-running imports from lost
 * RabbitMQ deliveries or service restarts.
 */
@Service
@ConditionalOnProperty(prefix = "cnm.import.recovery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CnmImportRecoveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CnmImportRecoveryService.class);
    private static final String TRANSFORM_INITIALIZATION_FILE_KEY = "__transform_init__";

    private final DocumentRepositoryService<CnmImportDocument> importRepository;
    private final EventPublisherService eventPublisher;
    private final String eventExchange;
    private final String transformInitializationRoutingKey;
    private final String fileProcessingRoutingKey;
    private final long staleAfterMillis;
    private final long requeueThrottleMillis;
    private final int scanLimit;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentMap<String, Long> requeueTimes = new ConcurrentHashMap<>();

    public CnmImportRecoveryService(
            InfrastructureUtils infrastructureUtils,
            @Value("${cnm.import.event.exchange:cnm.events}") String eventExchange,
            @Value("${cnm.import.event.transform-initialization-routing-key:cnm.transform.initialization.requested}")
                    String transformInitializationRoutingKey,
            @Value("${cnm.import.event.file-processing-routing-key:cnm.file.processing.requested}")
                    String fileProcessingRoutingKey,
            @Value("${cnm.import.recovery.stale-after-ms:30000}") long staleAfterMillis,
            @Value("${cnm.import.recovery.requeue-throttle-ms:30000}") long requeueThrottleMillis,
            @Value("${cnm.import.recovery.scan-limit:500}") int scanLimit) {
        this.importRepository = infrastructureUtils.documentRepository(new CnmImportDocumentAdapter());
        this.eventPublisher = infrastructureUtils.eventPublisher();
        this.eventExchange = eventExchange;
        this.transformInitializationRoutingKey = transformInitializationRoutingKey;
        this.fileProcessingRoutingKey = fileProcessingRoutingKey;
        this.staleAfterMillis = staleAfterMillis;
        this.requeueThrottleMillis = requeueThrottleMillis;
        this.scanLimit = scanLimit <= 0 ? 500 : scanLimit;
    }

    /**
     * Scans recent non-terminal imports and republishes missing asynchronous
     * work. The scheduler intentionally does not update import state; state is
     * advanced only by the normal event handlers that complete the work.
     */
    @Scheduled(fixedDelayString = "${cnm.import.recovery.fixed-delay-ms:30000}")
    public void recoverStaleImports() {
        if (!running.compareAndSet(false, true)) {
            LOGGER.debug("Skipping CNM import recovery because the previous scan is still running");
            return;
        }
        try {
            importRepository.findAll(scanLimit, DocumentSort.descending("createdAt"))
                    .forEach(this::recoverImport);
        } catch (RuntimeException exception) {
            LOGGER.warn("CNM import recovery scan failed", exception);
        } finally {
            running.set(false);
        }
    }

    void recoverImport(CnmImportDocument document) {
        if (document == null || document.state() == ImportState.SUCCESS || document.state() == ImportState.FAILED) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        if (document.state() == ImportState.STARTED && hasStaleStoredFile(document.files(), now)) {
            requeueTransformInitialization(document, now);
            return;
        }
        if (document.state() == ImportState.IN_PROGRESS) {
            document.files().stream()
                    .filter(file -> file.state() == ImportFileState.STORED)
                    .filter(file -> isStale(file, now))
                    .forEach(file -> requeueFileProcessing(document, file, now));
        }
    }

    private boolean hasStaleStoredFile(List<CnmImportFileDocument> files, long now) {
        return files.stream()
                .anyMatch(file -> file.state() == ImportFileState.STORED && isStale(file, now));
    }

    private void requeueTransformInitialization(CnmImportDocument document, long now) {
        if (!shouldRequeue(document.id(), TRANSFORM_INITIALIZATION_FILE_KEY, now)) {
            return;
        }
        try {
            eventPublisher.publish(
                    eventExchange,
                    transformInitializationRoutingKey,
                    new CnmTransformInitializationRequested(
                            document.id(),
                            document.serviceType(),
                            document.timeFrame(),
                            1,
                            Instant.now()));
            LOGGER.info("Re-queued stale CNM transform-initialization event for import {}", document.id());
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to re-queue stale CNM transform-initialization event for import {}", document.id(), exception);
        }
    }

    private void requeueFileProcessing(CnmImportDocument document, CnmImportFileDocument file, long now) {
        if (!shouldRequeue(document.id(), file.fileId(), now)) {
            return;
        }
        try {
            eventPublisher.publish(
                    eventExchange,
                    fileProcessingRoutingKey,
                    new CnmFileProcessingRequested(
                            document.id(),
                            file.fileId(),
                            file.objectId(),
                            file.fileName(),
                            document.serviceType(),
                            document.timeFrame(),
                            modelGroupKey(file),
                            isBoundaryProfile(file),
                            1,
                            Instant.now()));
            LOGGER.info("Re-queued stale CNM file-processing event for import {} file {}", document.id(), file.fileId());
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Unable to re-queue stale CNM file-processing event for import {} file {}",
                    document.id(),
                    file.fileId(),
                    exception);
        }
    }

    private boolean isStale(CnmImportFileDocument file, long now) {
        try {
            Instant uploadedAt = instant(file.uploadedAt());
            return uploadedAt == null || now - uploadedAt.toEpochMilli() >= staleAfterMillis;
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Treating CNM import file {} as stale because uploadedAt could not be parsed",
                    file.fileId(),
                    exception);
            return true;
        }
    }

    private boolean shouldRequeue(String importId, String fileId, long now) {
        String key = importId + ":" + fileId;
        Long lastRequeuedAt = requeueTimes.get(key);
        if (lastRequeuedAt != null && now - lastRequeuedAt < requeueThrottleMillis) {
            return false;
        }
        requeueTimes.put(key, now);
        return true;
    }

    private String modelGroupKey(CnmImportFileDocument file) {
        return valueOr(file.tsoName())
                + "|"
                + valueOr(file.businessDay())
                + "|"
                + valueOr(file.businessTime())
                + "|"
                + valueOr(file.modelTimeFrame());
    }

    private boolean isBoundaryProfile(CnmImportFileDocument file) {
        CgmesProfileKind kind = CgmesProfileKind.fromCode(file.profileType());
        return kind == CgmesProfileKind.BOUNDARY_EQUIPMENT || kind == CgmesProfileKind.BOUNDARY_TOPOLOGY;
    }

    private String valueOr(String value) {
        return value == null ? "" : value;
    }

    private Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        String text = value.toString().trim();
        if (text.matches("-?\\d+")) {
            return Instant.ofEpochMilli(Long.parseLong(text));
        }
        return Instant.parse(text);
    }
}
