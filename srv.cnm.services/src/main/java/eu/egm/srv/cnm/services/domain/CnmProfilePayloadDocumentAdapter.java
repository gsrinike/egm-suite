package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Maps large profile payload documents to their dedicated Elasticsearch index.
 */
public class CnmProfilePayloadDocumentAdapter implements DocumentAdapter<CnmProfilePayloadDocument> {
    @Override
    public String indexName() {
        return "cnm-profile-payloads";
    }

    @Override
    public String documentId(CnmProfilePayloadDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmProfilePayloadDocument> documentType() {
        return CnmProfilePayloadDocument.class;
    }
}
