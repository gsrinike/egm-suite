package eu.egm.data.cnm.common;

import java.util.List;

/**
 * Client-reported failure for an import request that could not reach or complete
 * the multipart upload endpoint.
 */
public record ImportFailureRequest(
        String importId,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        List<String> fileNames,
        String message) {
    public ImportFailureRequest {
        fileNames = fileNames == null ? List.of() : List.copyOf(fileNames);
    }
}
