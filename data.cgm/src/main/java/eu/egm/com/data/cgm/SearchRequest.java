package eu.egm.com.data.cgm;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SearchRequest(
        String query,
        EquipmentType type,
        String containerId,
        CgmesRegion region,
        CgmesProcess process,
        String businessDay,
        String timestamp,
        String timeFrame,
        String tsoName,
        String cgmesProfileType,
        String versionNumber,
        String extension,
        @Min(0) int page,
        @Min(1) @Max(200) int size
) {
    public SearchRequest {
        if (size == 0) {
            size = 50;
        }
    }
}
