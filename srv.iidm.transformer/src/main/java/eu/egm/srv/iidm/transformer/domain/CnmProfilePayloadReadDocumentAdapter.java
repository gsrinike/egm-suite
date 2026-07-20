package eu.egm.srv.iidm.transformer.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmProfilePayloadReadDocumentAdapter implements DocumentAdapter<CnmProfilePayloadReadDocument> {
    @Override
    public String indexName() {
        return "cnm-profile-payloads";
    }

    @Override
    public String documentId(CnmProfilePayloadReadDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmProfilePayloadReadDocument> documentType() {
        return CnmProfilePayloadReadDocument.class;
    }
}
