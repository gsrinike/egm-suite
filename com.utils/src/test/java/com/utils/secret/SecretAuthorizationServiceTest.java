package com.utils.secret;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecretAuthorizationServiceTest {
    @Test
    void allowsConfiguredClientAndKey() {
        SecretAuthorizationService service = new SecretAuthorizationService(Map.of(
                SecretAuthorizationService.CLIENT_ID_PROPERTY, "srv.cgm.importer",
                SecretAuthorizationService.ALLOWED_KEYS_PROPERTY, "MINIO_SECRET_KEY"), "other");

        SecretAccessDecision decision = service.authorize(new SecretAccessRequest("srv.cgm.importer", "MINIO_SECRET_KEY"));

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void keepsApplicationIdAsCompatibilityAlias() {
        SecretAuthorizationService service = new SecretAuthorizationService(Map.of(
                SecretAuthorizationService.APPLICATION_ID_PROPERTY, "srv.cgm.importer",
                SecretAuthorizationService.ALLOWED_KEYS_PROPERTY, "MINIO_SECRET_KEY"), "other");

        SecretAccessDecision decision = service.authorize(new SecretAccessRequest("srv.cgm.importer", "MINIO_SECRET_KEY"));

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void deniesUnlistedKey() {
        SecretAuthorizationService service = new SecretAuthorizationService(Map.of(
                SecretAuthorizationService.CLIENT_ID_PROPERTY, "srv.cgm.importer",
                SecretAuthorizationService.ALLOWED_KEYS_PROPERTY, "MINIO_SECRET_KEY"), "other");

        SecretAccessDecision decision = service.authorize(new SecretAccessRequest("srv.cgm.importer", "OTHER_KEY"));

        assertThat(decision.allowed()).isFalse();
    }
}
