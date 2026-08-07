package eu.egm.srv.cnm.services.service;

import eu.egm.data.cnm.cgmes.CgmesProfileKind;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.TimeFrame;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CGMES/CIM model file names into stable import metadata.
 *
 * <p>CGMES operational files normally use
 * {@code <timestamp>_<timeframe>_<tso>_<profile>_<version>}. Boundary and GL
 * data can omit the timeframe and use a double underscore, for example
 * {@code 20241203T0430Z__ENTSOE_EQBD_031.zip} or
 * {@code 20241203T0030Z__CEPS_GL_001.xml.zip}. This parser accepts both forms
 * and strips chained transport/data extensions before matching.</p>
 */
final class CnmModelFileNameParser {
    private static final Pattern MODEL_FILE_PATTERN =
            Pattern.compile(
                    "^(?<timestamp>\\d{8}T\\d{4}Z)_(?<timeFrame>[A-Z0-9]+)_(?<tso>.+?)_(?<profile>[A-Z0-9_-]+)_(?<version>\\d+)$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern NO_TIMEFRAME_MODEL_FILE_PATTERN =
            Pattern.compile(
                    "^(?<timestamp>\\d{8}T\\d{4}Z)__+(?<tso>.+?)_(?<profile>[A-Z0-9_-]+)_(?<version>\\d+)$",
                    Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter BASIC_TIME = DateTimeFormatter.ofPattern("HHmm");

    private CnmModelFileNameParser() {
    }

    static CnmModelFileName parse(String fileName) {
        return parse(fileName, null);
    }

    static CnmModelFileName parse(String fileName, TimeFrame fallbackTimeFrame) {
        String stem = stem(fileName);
        Matcher matcher = MODEL_FILE_PATTERN.matcher(stem);
        if (matcher.matches()) {
            return modelFileName(
                    matcher.group("timestamp"),
                    matcher.group("timeFrame"),
                    matcher.group("tso"),
                    matcher.group("profile"),
                    matcher.group("version"));
        }
        Matcher noTimeFrameMatcher = NO_TIMEFRAME_MODEL_FILE_PATTERN.matcher(stem);
        if (noTimeFrameMatcher.matches()) {
            return modelFileName(
                    noTimeFrameMatcher.group("timestamp"),
                    fallbackTimeFrameCode(fallbackTimeFrame),
                    noTimeFrameMatcher.group("tso"),
                    noTimeFrameMatcher.group("profile"),
                    noTimeFrameMatcher.group("version"));
        }
        return CnmModelFileName.empty();
    }

    private static CnmModelFileName modelFileName(
            String timestamp,
            String timeFrame,
            String tsoName,
            String profile,
            String version) {
        String profileType = normalizeProfileType(profile);
        return new CnmModelFileName(
                LocalDate.parse(timestamp.substring(0, 8), BASIC_DATE).toString(),
                LocalTime.parse(timestamp.substring(9, 13), BASIC_TIME).toString(),
                valueOr(timeFrame).toUpperCase(Locale.ROOT),
                valueOr(tsoName),
                profileType,
                valueOr(version),
                ProfileFamily.fromCode(profileType));
    }

    private static String normalizeProfileType(String profile) {
        String normalized = valueOr(profile).toUpperCase(Locale.ROOT).replace('-', '_');
        CgmesProfileKind kind = CgmesProfileKind.fromCode(normalized);
        return kind == CgmesProfileKind.UNKNOWN ? normalized : kind.code();
    }

    private static String fallbackTimeFrameCode(TimeFrame timeFrame) {
        if (timeFrame == null) {
            return "";
        }
        return switch (timeFrame) {
            case ID -> "ID";
            case DAY_AHEAD -> "1D";
            case TWO_DAYS_AHEAD -> "2D";
        };
    }

    private static String stem(String fileName) {
        String baseName = baseName(fileName);
        String stem = baseName;
        boolean stripped;
        do {
            stripped = false;
            for (String extension : new String[] {".zip", ".xml", ".rdf", ".idm", ".owl"}) {
                if (stem.toLowerCase(Locale.ROOT).endsWith(extension)) {
                    stem = stem.substring(0, stem.length() - extension.length());
                    stripped = true;
                    break;
                }
            }
        } while (stripped);
        return stem;
    }

    private static String baseName(String path) {
        int slash = path == null ? -1 : Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : valueOr(path);
    }

    private static String valueOr(String value) {
        return value == null ? "" : value.trim();
    }

    record CnmModelFileName(
            String businessDay,
            String businessTime,
            String timeFrame,
            String tsoName,
            String profileType,
            String version,
            ProfileFamily profileFamily) {
        static CnmModelFileName empty() {
            return new CnmModelFileName("", "", "", "", "", "", ProfileFamily.Unknown);
        }
    }
}
