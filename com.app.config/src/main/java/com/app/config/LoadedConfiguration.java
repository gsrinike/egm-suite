package com.app.config;

import java.util.Map;

public record LoadedConfiguration(String name, Map<String, Object> properties) {
}
