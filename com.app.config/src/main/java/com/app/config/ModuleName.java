package com.app.config;

public final class ModuleName {

    public static final String SYSTEM_PROPERTY = "module";
    public static final String ENVIRONMENT_VARIABLE = "MODULE";

    private ModuleName() {
    }

    public static String resolve() {
        String property = System.getProperty(SYSTEM_PROPERTY);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String environment = System.getenv(ENVIRONMENT_VARIABLE);
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }
        throw new IllegalStateException("Module name is not configured. Set module or MODULE.");
    }
}
