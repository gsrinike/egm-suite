package eu.egm.auth.model;

import java.util.List;
import java.util.Map;

public record AdminUserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Map<String, List<String>> attributes
) {
}
