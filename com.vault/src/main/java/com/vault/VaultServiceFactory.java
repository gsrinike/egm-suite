package com.vault;

import com.utils.secret.SecretAuthorizationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VaultServiceFactory {
    private VaultServiceFactory() {
    }

    public static VaultService create(Map<String, Object> vaultProperties,
                                      Map<String, String> environment,
                                      Map<String, Object> fallbackConfiguration) {
        return create(vaultProperties, environment, fallbackConfiguration, null);
    }

    public static VaultService create(Map<String, Object> vaultProperties,
                                      Map<String, String> environment,
                                      Map<String, Object> fallbackConfiguration,
                                      String clientId) {
        SecretAuthorizationService authorizationService = new SecretAuthorizationService(vaultProperties, clientId);
        VaultService fallback = authorize(new EnvironmentVaultService(environment, fallbackConfiguration),
                authorizationService,
                clientId);
        VaultConfiguration configuration = VaultConfiguration.from(vaultProperties, environment);
        if (!configuration.canConnect()) {
            return fallback;
        }
        List<VaultService> services = new ArrayList<>();
        services.add(authorize(new HashicorpVaultService(configuration), authorizationService, clientId));
        services.add(fallback);
        return new CompositeVaultService(services);
    }

    private static VaultService authorize(VaultService service,
                                          SecretAuthorizationService authorizationService,
                                          String clientId) {
        return new AuthorizedVaultService(service, authorizationService, clientId);
    }
}
