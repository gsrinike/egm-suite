package eu.egm.srv.iidm.transformer.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmNetworkSnapshotReadDocumentAdapter implements DocumentAdapter<CnmNetworkSnapshotReadDocument> {
    @Override
    public String indexName() {
        return "cnm-network-snapshots";
    }

    @Override
    public String documentId(CnmNetworkSnapshotReadDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmNetworkSnapshotReadDocument> documentType() {
        return CnmNetworkSnapshotReadDocument.class;
    }
}
