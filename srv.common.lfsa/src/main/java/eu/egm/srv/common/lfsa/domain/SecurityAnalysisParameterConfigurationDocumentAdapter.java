package eu.egm.srv.common.lfsa.domain;

import com.infra.storage.document.DocumentAdapter;

/**
 * Adapter for LFSA security-analysis parameter configuration documents.
 */
public class SecurityAnalysisParameterConfigurationDocumentAdapter
        implements DocumentAdapter<SecurityAnalysisParameterConfigurationDocument> {
    @Override
    public String indexName() {
        return "lfsa-security-analysis-parameters";
    }

    @Override
    public String documentId(SecurityAnalysisParameterConfigurationDocument document) {
        return document.id();
    }

    @Override
    public Class<SecurityAnalysisParameterConfigurationDocument> documentType() {
        return SecurityAnalysisParameterConfigurationDocument.class;
    }
}
