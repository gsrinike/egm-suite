package com.infra;

import com.infra.document.DocumentAdapter;
import com.infra.document.DocumentRepositoryService;
import com.infra.event.EventPublisherService;
import com.infra.storage.ObjectStorageService;

/**
 * Factory boundary used by service modules to obtain infrastructure adapters.
 *
 * The consumer supplies application-specific adapters where needed, and the
 * utility module chooses the concrete technology implementation.
 */
public interface InfrastructureAdapterFactory {
    <T> DocumentRepositoryService<T> documentRepository(DocumentAdapter<T> adapter);

    ObjectStorageService objectStorageService();

    EventPublisherService eventPublisher();
}
