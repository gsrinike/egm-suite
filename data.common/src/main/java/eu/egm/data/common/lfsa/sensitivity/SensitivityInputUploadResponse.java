package eu.egm.data.common.lfsa.sensitivity;

/**
 * Object-storage reference returned after uploading a sensitivity-analysis input file.
 */
public record SensitivityInputUploadResponse(
        String kind,
        String fileName,
        String objectId,
        long size) {
}
