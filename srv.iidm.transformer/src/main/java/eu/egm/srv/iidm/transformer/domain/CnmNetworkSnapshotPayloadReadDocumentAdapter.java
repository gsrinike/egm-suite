package eu.egm.srv.iidm.transformer.domain;

import com.infra.storage.document.DocumentAdapter;

public class CnmNetworkSnapshotPayloadReadDocumentAdapter implements DocumentAdapter<CnmNetworkSnapshotPayloadReadDocument> {
    @Override
    public String indexName() {
        return "cnm-network-snapshot-payloads";
    }

    @Override
    public String documentId(CnmNetworkSnapshotPayloadReadDocument document) {
        return document.id();
    }

    @Override
    public Class<CnmNetworkSnapshotPayloadReadDocument> documentType() {
        return CnmNetworkSnapshotPayloadReadDocument.class;
    }
}
