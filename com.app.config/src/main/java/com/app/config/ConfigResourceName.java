package com.app.config;

public record ConfigResourceName(String moduleName, String group, String qualifier, String extension) {

    public String classpathLocation() {
        return "%s/%s-%s.%s".formatted(qualifier, moduleName, group, extension);
    }
}
