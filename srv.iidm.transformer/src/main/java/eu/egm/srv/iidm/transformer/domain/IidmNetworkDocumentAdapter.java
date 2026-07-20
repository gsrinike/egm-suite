package eu.egm.srv.iidm.transformer.domain;

import com.infra.storage.document.DocumentAdapter;

public class IidmNetworkDocumentAdapter implements DocumentAdapter<IidmNetworkDocument> {
    @Override
    public String indexName() {
        return "iidm-networks";
    }

    @Override
    public String documentId(IidmNetworkDocument document) {
        return document.id();
    }

    @Override
    public Class<IidmNetworkDocument> documentType() {
        return IidmNetworkDocument.class;
    }
}
