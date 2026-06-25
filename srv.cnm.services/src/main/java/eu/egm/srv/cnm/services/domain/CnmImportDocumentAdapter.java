package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmImportDocumentAdapter implements DocumentAdapter<CnmImportDocument> {
    @Override
    public String indexName() {
        return "cnm-imports";
    }

    @Override
    public String documentId(CnmImportDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmImportDocument> documentType() {
        return CnmImportDocument.class;
    }
}
