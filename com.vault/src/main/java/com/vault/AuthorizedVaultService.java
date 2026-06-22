package com.vault;

import eu.egm.auth.secret.SecretAccessDecision;
import eu.egm.auth.secret.SecretAccessRequest;
import eu.egm.auth.secret.SecretAuthorizationService;

import java.util.Optional;

public class AuthorizedVaultService implements VaultService {
    private final VaultService delegate;
    private final SecretAuthorizationService authorizationService;
    private final String applicationId;

    public AuthorizedVaultService(VaultService delegate,
                                  SecretAuthorizationService authorizationService,
                                  String applicationId) {
        this.delegate = delegate;
        this.authorizationService = authorizationService;
        this.applicationId = applicationId;
    }

    @Override
    public Optional<String> getSecret(String key) {
        SecretAccessDecision decision = authorizationService.authorize(new SecretAccessRequest(applicationId, key));
        if (!decision.allowed()) {
            throw new SecurityException("Vault secret access denied for key " + key + ": " + decision.reason());
        }
        return delegate.getSecret(key);
    }
}
