package eu.egm.srv.cnm.services.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmNetworkSnapshotPayloadDocumentAdapter implements DocumentAdapter<CnmNetworkSnapshotPayloadDocument> {
    @Override
    public String indexName() {
        return "cnm-network-snapshot-payloads";
    }

    @Override
    public String documentId(CnmNetworkSnapshotPayloadDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmNetworkSnapshotPayloadDocument> documentType() {
        return CnmNetworkSnapshotPayloadDocument.class;
    }
}
