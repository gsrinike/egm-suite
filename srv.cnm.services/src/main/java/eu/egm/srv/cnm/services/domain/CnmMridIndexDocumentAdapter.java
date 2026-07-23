package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmMridIndexDocumentAdapter implements DocumentAdapter<CnmMridIndexDocument> {
    @Override
    public String indexName() {
        return "cnm-mrid-index";
    }

    @Override
    public String documentId(CnmMridIndexDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmMridIndexDocument> documentType() {
        return CnmMridIndexDocument.class;
    }
}
