package eu.egm.srv.iidm.transformer.domain;

import com.infra.storage.document.DocumentAdapter;

public class IidmGridViewDocumentAdapter implements DocumentAdapter<IidmGridViewDocument> {
    @Override
    public String indexName() {
        return "iidm-grid-view-maps";
    }

    @Override
    public String documentId(IidmGridViewDocument document) {
        return document.id();
    }

    @Override
    public Class<IidmGridViewDocument> documentType() {
        return IidmGridViewDocument.class;
    }
}
