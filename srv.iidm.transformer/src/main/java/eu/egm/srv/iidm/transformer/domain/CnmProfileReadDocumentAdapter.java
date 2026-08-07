package eu.egm.srv.iidm.transformer.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmProfileReadDocumentAdapter implements DocumentAdapter<CnmProfileReadDocument> {
    @Override
    public String indexName() {
        return "cnm-profiles";
    }

    @Override
    public String documentId(CnmProfileReadDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmProfileReadDocument> documentType() {
        return CnmProfileReadDocument.class;
    }
}
