package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmProfileFragmentDocumentAdapter implements DocumentAdapter<CnmProfileFragmentDocument> {
    @Override
    public String indexName() {
        return "cnm-profile-fragments";
    }

    @Override
    public String documentId(CnmProfileFragmentDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmProfileFragmentDocument> documentType() {
        return CnmProfileFragmentDocument.class;
    }
}
