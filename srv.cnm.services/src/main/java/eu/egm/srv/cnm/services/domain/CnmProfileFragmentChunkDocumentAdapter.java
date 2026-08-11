package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Maps RDF fragment chunk documents to their dedicated Elasticsearch index.
 */
public class CnmProfileFragmentChunkDocumentAdapter implements DocumentAdapter<CnmProfileFragmentChunkDocument> {
    @Override
    public String indexName() {
        return "cnm-profile-fragment-chunks";
    }

    @Override
    public String documentId(CnmProfileFragmentChunkDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmProfileFragmentChunkDocument> documentType() {
        return CnmProfileFragmentChunkDocument.class;
    }
}
