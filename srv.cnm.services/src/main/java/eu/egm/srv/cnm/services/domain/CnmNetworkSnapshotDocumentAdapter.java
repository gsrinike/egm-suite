package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmNetworkSnapshotDocumentAdapter implements DocumentAdapter<CnmNetworkSnapshotDocument> {
    @Override
    public String indexName() {
        return "cnm-network-snapshots";
    }

    @Override
    public String documentId(CnmNetworkSnapshotDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmNetworkSnapshotDocument> documentType() {
        return CnmNetworkSnapshotDocument.class;
    }
}
