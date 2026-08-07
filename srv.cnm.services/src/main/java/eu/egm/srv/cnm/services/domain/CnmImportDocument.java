package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.IidmTransformationStatus;
import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import eu.egm.data.cnm.common.TimeFrame;
import java.time.Instant;
import java.util.List;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Searchable import document persisted through the infrastructure document adapter.
 */
public record CnmImportDocument(
        @Field(type = FieldType.Keyword)
        String id,
        @Field(type = FieldType.Keyword)
        CnmServiceType serviceType,
        @Field(type = FieldType.Keyword)
        TimeFrame timeFrame,
        @Field(type = FieldType.Keyword)
        ImportState state,
        @Field(type = FieldType.Nested)
        List<CnmImportFileDocument> files,
        @Field(type = FieldType.Long)
        Long createdAt,
        @Field(type = FieldType.Text)
        String message,
        @Field(type = FieldType.Keyword)
        IidmTransformationStatus iidmTransformationStatus) {
    public CnmImportDocument(
            String id,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            ImportState state,
            List<CnmImportFileDocument> files,
            Object createdAt,
            String message,
            IidmTransformationStatus iidmTransformationStatus) {
        this(
                id,
                serviceType,
                timeFrame,
                state,
                files,
                epochMillis(createdAt),
                message,
                iidmTransformationStatus);
    }

    public CnmImportDocument(
            String id,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            ImportState state,
            List<CnmImportFileDocument> files,
            Long createdAt,
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
                epochMillis(createdAt),
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
            @Field(type = FieldType.Keyword)
            String fileId,
            @Field(type = FieldType.Keyword)
            String fileName,
            @Field(type = FieldType.Keyword)
            String objectId,
            @Field(type = FieldType.Keyword)
            ImportFileState state,
            @Field(type = FieldType.Keyword)
            ProfileFamily profileFamily,
            @Field(type = FieldType.Keyword)
            String businessDay,
            @Field(type = FieldType.Keyword)
            String businessTime,
            @Field(type = FieldType.Keyword)
            String modelTimeFrame,
            @Field(type = FieldType.Keyword)
            String tsoName,
            @Field(type = FieldType.Keyword)
            String profileType,
            @Field(type = FieldType.Keyword)
            String modelVersion,
            @Field(type = FieldType.Object)
            List<RdfProfileReference> profiles,
            @Field(type = FieldType.Text)
            String message,
            @Field(type = FieldType.Long)
            Long uploadedAt,
            @Field(type = FieldType.Keyword)
            IidmTransformationStatus iidmTransformationStatus,
            @Field(type = FieldType.Integer)
            Integer iidmTransformationCount,
            @Field(type = FieldType.Integer)
            Integer iidmTransformationCompletedCount,
            @Field(type = FieldType.Integer)
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
                Object uploadedAt,
                IidmTransformationStatus iidmTransformationStatus,
                Integer iidmTransformationCount,
                Integer iidmTransformationCompletedCount,
                Integer iidmTransformationFailedCount) {
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
                    epochMillis(uploadedAt),
                    iidmTransformationStatus,
                    iidmTransformationCount,
                    iidmTransformationCompletedCount,
                    iidmTransformationFailedCount);
        }

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
                Long uploadedAt) {
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
                    epochMillis(uploadedAt));
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

    private static Long epochMillis(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof Instant instant) {
            return instant.toEpochMilli();
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return null;
        }
        if (text.matches("-?\\d+")) {
            return Long.parseLong(text);
        }
        return Instant.parse(text).toEpochMilli();
    }
}
