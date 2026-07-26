package eu.egm.srv.common.lfsa.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Adapter for reading IIDM network documents from the IIDM-owned index.
 */
public class IidmNetworkReadDocumentAdapter implements DocumentAdapter<IidmNetworkReadDocument> {
    @Override
    public String indexName() {
        return "iidm-networks";
    }

    @Override
    public String documentId(IidmNetworkReadDocument document) {
        return document.id();
    }

    @Override
    public Class<IidmNetworkReadDocument> documentType() {
        return IidmNetworkReadDocument.class;
    }
}
