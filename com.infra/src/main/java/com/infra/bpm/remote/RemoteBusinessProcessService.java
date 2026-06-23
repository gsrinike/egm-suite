package com.infra.bpm.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infra.bpm.BusinessProcessService;
import com.infra.bpm.ProcessInstance;
import com.infra.bpm.ProcessMessage;
import com.infra.bpm.ProcessMessageResult;
import com.infra.bpm.ProcessStartRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class RemoteBusinessProcessService implements BusinessProcessService {
    private final URI baseUri;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RemoteBusinessProcessService(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUri = URI.create(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProcessInstance start(ProcessStartRequest request) {
        return send("POST", "/api/bpm/cgm-imports/start", request.variables().get("importStatus"), ProcessInstance.class);
    }

    @Override
    public void cancel(String processInstanceId) {
        send("POST", "/api/bpm/cgm-imports/instances/" + encode(processInstanceId) + "/cancel", null, Void.class);
    }

    @Override
    public ProcessMessageResult correlateMessage(ProcessMessage message) {
        return send("POST", "/api/bpm/cgm-imports/callback", message, ProcessMessageResult.class);
    }

    @Override
    public Optional<ProcessInstance> findProcessInstance(String processInstanceId) {
        try {
            return Optional.of(send("GET", "/api/bpm/cgm-imports/instances/" + encode(processInstanceId), null, ProcessInstance.class));
        } catch (IllegalStateException exception) {
            if (exception.getMessage().contains("status 404")) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private <T> T send(String method, String path, Object body, Class<T> responseType) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path));
            if ("GET".equals(method)) {
                builder.GET();
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : objectMapper.writeValueAsString(body)))
                        .header("Content-Type", "application/json");
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Remote BPM call failed with status " + response.statusCode());
            }
            if (responseType == Void.class) {
                return null;
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("Remote BPM call failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Remote BPM call interrupted", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
