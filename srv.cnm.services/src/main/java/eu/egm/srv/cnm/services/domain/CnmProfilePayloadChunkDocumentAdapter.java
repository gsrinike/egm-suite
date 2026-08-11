package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Maps profile payload chunk documents to their dedicated Elasticsearch index.
 */
public class CnmProfilePayloadChunkDocumentAdapter implements DocumentAdapter<CnmProfilePayloadChunkDocument> {
    @Override
    public String indexName() {
        return "cnm-profile-payload-chunks";
    }

    @Override
    public String documentId(CnmProfilePayloadChunkDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmProfilePayloadChunkDocument> documentType() {
        return CnmProfilePayloadChunkDocument.class;
    }
}
