package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import eu.egm.data.cnm.common.TimeFrame;
import java.util.List;

/**
 * Searchable import document persisted through the infrastructure document adapter.
 */
public record CnmImportDocument(
        String id,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        ImportState state,
        List<CnmImportFileDocument> files,
        Object createdAt,
        String message) {
    public CnmImportDocument {
        files = files == null ? List.of() : List.copyOf(files);
    }

    public record CnmImportFileDocument(
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
            List<RdfProfileReference> profiles,
            String message,
            Object uploadedAt) {
        public CnmImportFileDocument {
            profiles = profiles == null ? List.of() : List.copyOf(profiles);
        }
    }
}
