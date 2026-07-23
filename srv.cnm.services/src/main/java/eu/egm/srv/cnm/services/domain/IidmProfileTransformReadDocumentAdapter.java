package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Adapter for reading IIDM transform status from the IIDM-owned index.
 */
public class IidmProfileTransformReadDocumentAdapter implements DocumentAdapter<IidmProfileTransformReadDocument> {
    @Override
    public String indexName() {
        return "iidm-profile-transforms";
    }

    @Override
    public String documentId(IidmProfileTransformReadDocument document) {
        return document.id();
    }

    @Override
    public Class<IidmProfileTransformReadDocument> documentType() {
        return IidmProfileTransformReadDocument.class;
    }
}
