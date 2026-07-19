package eu.egm.mapping;

/**
 * Converts DTOs to JSON and back without exposing a JSON library to service modules.
 */
public interface JsonMappingService {
    String toJson(Object value);

    <T> T fromJson(String json, Class<T> targetType);
}
