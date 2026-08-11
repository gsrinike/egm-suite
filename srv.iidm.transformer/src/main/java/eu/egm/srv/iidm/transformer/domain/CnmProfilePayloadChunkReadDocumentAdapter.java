package eu.egm.srv.iidm.transformer.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmProfilePayloadChunkReadDocumentAdapter implements DocumentAdapter<CnmProfilePayloadChunkReadDocument> {
    @Override
    public String indexName() {
        return "cnm-profile-payload-chunks";
    }

    @Override
    public String documentId(CnmProfilePayloadChunkReadDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmProfilePayloadChunkReadDocument> documentType() {
        return CnmProfilePayloadChunkReadDocument.class;
    }
}
