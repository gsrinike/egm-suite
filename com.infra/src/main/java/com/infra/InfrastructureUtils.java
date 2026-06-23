package com.infra;

import com.infra.bpm.BusinessProcessService;
import com.infra.event.EventPublisherService;
import com.infra.storage.document.DocumentAdapter;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.object.ObjectStorageService;

/**
 * Factory boundary used by service modules to obtain infrastructure adapters.
 *
 * The consumer supplies application-specific adapters where needed, and the
 * utility module chooses the concrete technology implementation.
 */
public interface InfrastructureUtils {
    <T> DocumentRepositoryService<T> documentRepository(DocumentAdapter<T> adapter);

    ObjectStorageService objectStorageService();

    EventPublisherService eventPublisher();

    BusinessProcessService businessProcessService();
}
