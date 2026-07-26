package eu.egm.srv.common.lfsa.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Adapter for reading CNM import documents from the CNM-owned index.
 */
public class CnmImportReadDocumentAdapter implements DocumentAdapter<CnmImportReadDocument> {
    @Override
    public String indexName() {
        return "cnm-imports";
    }

    @Override
    public String documentId(CnmImportReadDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmImportReadDocument> documentType() {
        return CnmImportReadDocument.class;
    }
}
