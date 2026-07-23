package eu.egm.srv.cnm.services.rdf;

import com.utils.profile.ProfileDefaults;
import eu.egm.data.cnm.common.ProfileFamily;

/**
 * Processing context for one RDF profile file.
 *
 * <p>The context carries the import/file identity, model grouping attributes,
 * detected profile classification, and cached profile-default configuration.
 * The queue key groups files by import, TSO, business day, business time, and
 * timeframe so cross-referenced CGMES files for the same model are processed
 * serially while unrelated model groups can still run in parallel.</p>
 */
public record ProfileProcessingContext(
        String importId,
        String fileId,
        String objectId,
        String tsoName,
        String businessDay,
        String businessTime,
        String timeFrame,
        ProfileFamily profileFamily,
        String profileType,
        ProfileDefaults profileDefaults) {
    public ProfileProcessingContext {
        importId = valueOr(importId);
        fileId = valueOr(fileId);
        objectId = valueOr(objectId);
        tsoName = valueOr(tsoName);
        businessDay = valueOr(businessDay);
        businessTime = valueOr(businessTime);
        timeFrame = valueOr(timeFrame);
        profileFamily = profileFamily == null ? ProfileFamily.Unknown : profileFamily;
        profileType = valueOr(profileType);
    }

    public static ProfileProcessingContext forFile(
            String importId,
            String fileId,
            String objectId,
            String tsoName,
            String businessDay,
            String businessTime,
            String timeFrame,
            ProfileFamily profileFamily,
            String profileType) {
        return new ProfileProcessingContext(
                importId,
                fileId,
                objectId,
                tsoName,
                businessDay,
                businessTime,
                timeFrame,
                profileFamily,
                profileType,
                null);
    }

    public ProfileProcessingContext withDetectedProfile(
            ProfileFamily detectedFamily,
            String detectedProfileType,
            ProfileDefaults defaults) {
        return new ProfileProcessingContext(
                importId,
                fileId,
                objectId,
                tsoName,
                businessDay,
                businessTime,
                timeFrame,
                detectedFamily,
                detectedProfileType,
                defaults);
    }

    public String queueKey() {
        return String.join("|", importId, tsoName, businessDay, businessTime, timeFrame);
    }

    private static String valueOr(String value) {
        return value == null ? "" : value;
    }
}
