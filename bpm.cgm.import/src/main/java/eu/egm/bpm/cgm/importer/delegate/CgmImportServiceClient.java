package eu.egm.bpm.cgm.importer.delegate;

import eu.egm.data.cgm.dto.cgmes.ImportStatus;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CgmImportServiceClient {
    private final RestClient restClient;

    public CgmImportServiceClient(RestClient cgmImportRestClient) {
        this.restClient = cgmImportRestClient;
    }

    public ImportStatus transform(String networkId, String objectId) {
        return restClient.post()
                .uri("/api/cgm/imports/{networkId}/transforms/cgmes", networkId)
                .body(new CgmesTransformRequest(objectId))
                .retrieve()
                .body(ImportStatus.class);
    }

    public ImportStatus updateStatus(String networkId, String objectId, String status, String iidmStatus, List<String> documentIds, String message) {
        return restClient.post()
                .uri("/api/cgm/imports/{networkId}/statuses/files", networkId)
                .body(new ImportFileStatusUpdate(objectId, status, iidmStatus, documentIds, message))
                .retrieve()
                .body(ImportStatus.class);
    }

    public record CgmesTransformRequest(String objectId) {
    }

    public record ImportFileStatusUpdate(
            String objectId,
            String status,
            String iidmTransformStatus,
            List<String> documentIds,
            String message
    ) {
    }
}
