package eu.egm.srv.common.lfsa.domain;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.TimeFrame;
import java.util.List;

/**
 * Read-only projection of CNM imports consumed by LFSA.
 */
public record CnmImportReadDocument(
        String id,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        ImportState state,
        List<CnmImportFileReadDocument> files,
        Object createdAt,
        String message) {
    public CnmImportReadDocument {
        files = files == null ? List.of() : List.copyOf(files);
    }

    public record CnmImportFileReadDocument(
            String fileId,
            String fileName,
            String objectId,
            ImportFileState state,
            ProfileFamily profileFamily,
            String businessDay,
            String businessTime,
            String modelTimeFrame,
            String tsoName,
            String profileType,
            String modelVersion,
            Object profiles,
            String message,
            Object uploadedAt) {
    }
}
