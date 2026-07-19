package eu.egm.mapping;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonJsonMappingServiceTest {
    @Test
    void roundTripsProfilePayloadAsJsonObject() {
        JsonMappingService mappingService = new JacksonJsonMappingService();
        Map<String, Object> payload = Map.of(
                "profileType", "SV",
                "fileId", "file-1",
                "profile", Map.of("voltages", java.util.List.of(Map.of("mRID", "SV-1", "v", 400.0))));

        String json = mappingService.toJson(payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> restored = mappingService.fromJson(json, Map.class);

        assertThat(json).contains("\"profileType\":\"SV\"");
        assertThat(restored).containsEntry("fileId", "file-1");
        assertThat(restored.get("profile")).isInstanceOf(Map.class);
    }
}
