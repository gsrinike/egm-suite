package eu.egm.srv.iidm.transformer.domain;

import com.infra.storage.document.DocumentAdapter;

public class IidmProfileTransformDocumentAdapter implements DocumentAdapter<IidmProfileTransformDocument> {
    @Override
    public String indexName() {
        return "iidm-profile-transforms";
    }

    @Override
    public String documentId(IidmProfileTransformDocument document) {
        return document.id();
    }

    @Override
    public Class<IidmProfileTransformDocument> documentType() {
        return IidmProfileTransformDocument.class;
    }
}
