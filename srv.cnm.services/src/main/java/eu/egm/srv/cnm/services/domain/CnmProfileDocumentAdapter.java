package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Maps profile metadata to its dedicated Elasticsearch index.
 */
public class CnmProfileDocumentAdapter implements DocumentAdapter<CnmProfileDocument> {
    @Override
    public String indexName() {
        return "cnm-profiles";
    }

    @Override
    public String documentId(CnmProfileDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmProfileDocument> documentType() {
        return CnmProfileDocument.class;
    }
}
