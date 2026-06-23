package com.vault;

import com.utils.secret.SecretAccessDecision;
import com.utils.secret.SecretAccessRequest;
import com.utils.secret.SecretAuthorizationService;

import java.util.Optional;

/**
 * Vault decorator that checks whether the current client is allowed to read a
 * secret before delegating to the configured provider.
 */
public class AuthorizedVaultService implements VaultService {
    private final VaultService delegate;
    private final SecretAuthorizationService authorizationService;
    private final String clientId;

    public AuthorizedVaultService(VaultService delegate,
                                  SecretAuthorizationService authorizationService,
                                  String clientId) {
        this.delegate = delegate;
        this.authorizationService = authorizationService;
        this.clientId = clientId;
    }

    @Override
    public Optional<String> getSecret(String key) {
        SecretAccessDecision decision = authorizationService.authorize(new SecretAccessRequest(clientId, key));
        if (!decision.allowed()) {
            throw new SecurityException("Vault secret access denied for key " + key + ": " + decision.reason());
        }
        return delegate.getSecret(key);
    }
}
