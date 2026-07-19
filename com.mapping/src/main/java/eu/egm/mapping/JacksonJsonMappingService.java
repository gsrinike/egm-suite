package eu.egm.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson-backed implementation of the generic JSON mapping contract.
 */
public class JacksonJsonMappingService implements JsonMappingService {
    private final ObjectMapper objectMapper;

    public JacksonJsonMappingService() {
        this(new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    public JacksonJsonMappingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new MappingException("Unable to convert DTO to JSON", exception);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (Exception exception) {
            throw new MappingException("Unable to convert JSON to DTO", exception);
        }
    }
}
