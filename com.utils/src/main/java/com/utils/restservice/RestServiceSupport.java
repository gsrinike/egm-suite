package com.utils.restservice;

import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Shared base support for REST-facing service implementations.
 *
 * <p>Provides the service logger, Spring environment, and observation registry
 * without introducing application-specific defaults.</p>
 */
public abstract class RestServiceSupport {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Environment environment;
    protected final ObservationRegistry observationRegistry;

    protected RestServiceSupport(Environment environment, ObservationRegistry observationRegistry) {
        this.environment = environment;
        this.observationRegistry = observationRegistry == null ? ObservationRegistry.NOOP : observationRegistry;
    }

    /**
     * Returns the module name supplied through configuration or application bootstrap.
     */
    protected String moduleName() {
        return environment.getProperty("module", "unknown");
    }
}
