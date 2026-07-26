package eu.egm.srv.common.lfsa.domain.sensitivity;

import com.infra.storage.document.DocumentAdapter;

/**
 * Maps sensitivity run documents to their Elasticsearch index.
 */
public class SensitivityAnalysisRunDocumentAdapter implements DocumentAdapter<SensitivityAnalysisRunDocument> {
    @Override
    public String indexName() {
        return "sensitivity-runs";
    }

    @Override
    public String documentId(SensitivityAnalysisRunDocument document) {
        return document.id();
    }

    @Override
    public Class<SensitivityAnalysisRunDocument> documentType() {
        return SensitivityAnalysisRunDocument.class;
    }
}
