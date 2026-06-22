package com.vault;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HashicorpVaultService implements VaultService {
    private final VaultConfiguration configuration;
    private final HttpClient httpClient;

    public HashicorpVaultService(VaultConfiguration configuration) {
        this(configuration, HttpClient.newBuilder()
                .connectTimeout(configuration.timeout())
                .build());
    }

    HashicorpVaultService(VaultConfiguration configuration, HttpClient httpClient) {
        this.configuration = configuration;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<String> getSecret(String key) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(secretUri())
                .timeout(configuration.timeout())
                .GET()
                .header("X-Vault-Token", configuration.token());
        if (configuration.namespace() != null && !configuration.namespace().isBlank()) {
            builder.header("X-Vault-Namespace", configuration.namespace());
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Vault secret lookup failed with status " + response.statusCode());
            }
            return extractJsonString(response.body(), key);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read secret from Vault", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading secret from Vault", exception);
        }
    }

    private URI secretUri() {
        String normalizedAddress = trimRight(configuration.address().toString(), "/");
        String mount = trimSlashes(configuration.kvMount());
        String path = trimSlashes(configuration.kvPath());
        String versionSegment = configuration.kvVersion() == 2 ? "/data" : "";
        return URI.create(normalizedAddress + "/v1/" + mount + versionSegment + "/" + path);
    }

    private Optional<String> extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Optional.of(unescape(matcher.group(1))) : Optional.empty();
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String trimRight(String value, String suffix) {
        String result = value;
        while (result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }

    private String trimSlashes(String value) {
        String result = value == null ? "" : value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
