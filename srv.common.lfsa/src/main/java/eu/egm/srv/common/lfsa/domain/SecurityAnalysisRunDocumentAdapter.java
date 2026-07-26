package eu.egm.srv.common.lfsa.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Adapter for LFSA security-analysis run documents.
 */
public class SecurityAnalysisRunDocumentAdapter implements DocumentAdapter<SecurityAnalysisRunDocument> {
    @Override
    public String indexName() {
        return "lfsa-security-analysis-runs";
    }

    @Override
    public String documentId(SecurityAnalysisRunDocument document) {
        return document.id();
    }

    @Override
    public Class<SecurityAnalysisRunDocument> documentType() {
        return SecurityAnalysisRunDocument.class;
    }
}
