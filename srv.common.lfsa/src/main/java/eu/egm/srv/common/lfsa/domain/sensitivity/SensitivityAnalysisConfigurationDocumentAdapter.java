package eu.egm.srv.common.lfsa.domain.sensitivity;

import com.infra.storage.document.DocumentAdapter;

/**
 * Maps sensitivity configuration documents to their Elasticsearch index.
 */
public class SensitivityAnalysisConfigurationDocumentAdapter
        implements DocumentAdapter<SensitivityAnalysisConfigurationDocument> {
    @Override
    public String indexName() {
        return "sensitivity-configurations";
    }

    @Override
    public String documentId(SensitivityAnalysisConfigurationDocument document) {
        return document.id();
    }

    @Override
    public Class<SensitivityAnalysisConfigurationDocument> documentType() {
        return SensitivityAnalysisConfigurationDocument.class;
    }
}
