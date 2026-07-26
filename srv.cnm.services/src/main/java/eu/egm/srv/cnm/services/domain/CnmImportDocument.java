package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.IidmTransformationStatus;
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
        String message,
        IidmTransformationStatus iidmTransformationStatus) {
    public CnmImportDocument(
            String id,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            ImportState state,
            List<CnmImportFileDocument> files,
            Object createdAt,
            String message) {
        this(
                id,
                serviceType,
                timeFrame,
                state,
                files,
                createdAt,
                message,
                IidmTransformationStatus.NOT_STARTED);
    }

    public CnmImportDocument {
        files = files == null ? List.of() : List.copyOf(files);
        iidmTransformationStatus = iidmTransformationStatus == null
                ? IidmTransformationStatus.NOT_STARTED
                : iidmTransformationStatus;
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
            Object uploadedAt,
            IidmTransformationStatus iidmTransformationStatus,
            Integer iidmTransformationCount,
            Integer iidmTransformationCompletedCount,
            Integer iidmTransformationFailedCount) {
        public CnmImportFileDocument(
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
            this(
                    fileId,
                    fileName,
                    objectId,
                    state,
                    profileFamily,
                    businessDay,
                    businessTime,
                    modelTimeFrame,
                    tsoName,
                    profileType,
                    modelVersion,
                    profiles,
                    message,
                    uploadedAt,
                    IidmTransformationStatus.NOT_STARTED,
                    0,
                    0,
                    0);
        }

        public CnmImportFileDocument {
            profiles = profiles == null ? List.of() : List.copyOf(profiles);
            iidmTransformationStatus = iidmTransformationStatus == null
                    ? IidmTransformationStatus.NOT_STARTED
                    : iidmTransformationStatus;
            iidmTransformationCount = Math.max(iidmTransformationCount == null ? 0 : iidmTransformationCount, 0);
            iidmTransformationCompletedCount = Math.max(
                    iidmTransformationCompletedCount == null ? 0 : iidmTransformationCompletedCount,
                    0);
            iidmTransformationFailedCount = Math.max(
                    iidmTransformationFailedCount == null ? 0 : iidmTransformationFailedCount,
                    0);
        }
    }
}
