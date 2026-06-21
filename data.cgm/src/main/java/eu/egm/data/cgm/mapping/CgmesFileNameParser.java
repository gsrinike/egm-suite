package eu.egm.data.cgm.mapping;

import eu.egm.data.cgm.dto.cgmes.*;
import eu.egm.data.cgm.dto.iidm.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the business metadata encoded in CGMES exchange file names.
 *
 * Example accepted input:
 * {@code 20231016T0030Z_1D_TSCNET-EU-MAVIR_SSH_000.zip}
 */
public final class CgmesFileNameParser {
    private static final Pattern CGMES_NAME = Pattern.compile(
            "^(?<businessDay>\\d{8})T?(?<timestamp>\\d{4})Z_(?<timeFrame>[^_]+)_(?<tsoName>[^_]+)_(?<profile>[^_]+)_(?<version>[^.]+)\\.(?<extension>[^.]+)$",
            Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter BUSINESS_DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CgmesFileNameParser() {
    }

    public static ImportMetadata parse(String fileName, CgmesRegion region, CgmesProcess process) {
        Matcher matcher = CGMES_NAME.matcher(fileName == null ? "" : fileName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("CGMES file name must follow <yyyymmdd><hhmm>Z_<time_frame>_<TSO>_<profile>_<version>.<extension>");
        }
        String timestamp = matcher.group("timestamp");
        // Region and process are study context selected by the user; the rest is derived from the file name.
        return ImportMetadata.of(
                LocalDate.parse(matcher.group("businessDay"), BUSINESS_DAY_FORMATTER),
                timestamp.substring(0, 2) + ":" + timestamp.substring(2, 4),
                region,
                process,
                matcher.group("timeFrame").toUpperCase(Locale.ROOT),
                matcher.group("tsoName").toUpperCase(Locale.ROOT),
                matcher.group("profile").toUpperCase(Locale.ROOT),
                matcher.group("version"),
                matcher.group("extension").toLowerCase(Locale.ROOT));
    }
}
