package eu.egm.data.iidm.common;

import eu.egm.data.cnm.common.ProfileFamily;

/**
 * Event emitted after a CNM profile payload has been parsed and persisted.
 */
public record IidmProfileTransformRequested(
        String importId,
        String fileId,
        String sourceProfilePayloadId,
        String sourceSnapshotId,
        String profileType,
        ProfileFamily profileFamily,
        String objectId,
        String businessDay,
        String businessTime,
        String tsoName,
        String timeFrame) {
}
