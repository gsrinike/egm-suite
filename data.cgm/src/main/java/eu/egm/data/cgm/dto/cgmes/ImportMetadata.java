package eu.egm.data.cgm.dto.cgmes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Business and file context attached to imported equipment.
 *
 * Business day and timestamp come from the CGMES file name, while region and
 * process are selected by the user for the study being imported.
 */
public record ImportMetadata(
        @NotNull LocalDate businessDay,
        @NotNull LocalTime timestamp,
        @NotNull CgmesRegion region,
        @NotNull CgmesProcess process,
        String timeFrame,
        String tsoName,
        String cgmesProfileType,
        String versionNumber,
        String extension
) {
    private static final int QUARTER_HOUR_MINUTES = 15;

    public ImportMetadata {
        // CGMES snapshots in this application are expected on quarter-hour boundaries.
        if (timestamp != null && (timestamp.getMinute() % QUARTER_HOUR_MINUTES != 0 || timestamp.getSecond() != 0 || timestamp.getNano() != 0)) {
            throw new IllegalArgumentException("timestamp must be aligned to a 15-minute interval");
        }
    }

    public static ImportMetadata of(LocalDate businessDay, @Pattern(regexp = "\\d{2}:\\d{2}") String timestamp, CgmesRegion region, CgmesProcess process) {
        return new ImportMetadata(businessDay, LocalTime.parse(timestamp), region, process, "", "", "", "", "");
    }

    public static ImportMetadata of(LocalDate businessDay, @Pattern(regexp = "\\d{2}:\\d{2}") String timestamp,
                                    CgmesRegion region, CgmesProcess process, String timeFrame, String tsoName,
                                    String cgmesProfileType, String versionNumber, String extension) {
        return new ImportMetadata(businessDay, LocalTime.parse(timestamp), region, process,
                normalize(timeFrame), normalize(tsoName), normalize(cgmesProfileType), normalize(versionNumber), normalize(extension));
    }

    public ImportMetadata withStudyContext(CgmesRegion region, CgmesProcess process) {
        return new ImportMetadata(businessDay, timestamp, region, process, timeFrame, tsoName, cgmesProfileType, versionNumber, extension);
    }

    public CgmesProfileType powsyblProfileType() {
        return CgmesProfileType.fromCode(cgmesProfileType);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
