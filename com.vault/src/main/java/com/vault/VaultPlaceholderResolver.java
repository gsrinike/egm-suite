package com.vault;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VaultPlaceholderResolver {
    private static final Pattern VAULT_PATTERN = Pattern.compile("\\$\\{vault:([^}]+)}");

    private final VaultService vaultService;

    public VaultPlaceholderResolver(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public Map<String, Object> resolve(Map<String, Object> properties) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            resolved.put(entry.getKey(), resolveValue(entry.getValue()));
        }
        return resolved;
    }

    public Object resolveValue(Object value) {
        if (!(value instanceof String text)) {
            return value;
        }
        Matcher matcher = VAULT_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String secretKey = matcher.group(1).trim();
            String secret = vaultService.requireSecret(secretKey);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(secret));
        }
        if (!found) {
            return value;
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
