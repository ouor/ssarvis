package com.ssarvis.backend.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OpenAiClient {
    private static final Duration EXTERNAL_REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public OpenAiClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    public String requestChatCompletion(List<OpenAiMessage> messages) {
        String apiKey = appProperties.getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_API_KEY is not configured.");
        }

        try {
            String payload = objectMapper.writeValueAsString(new OpenAiChatCompletionRequest(
                    appProperties.getOpenai().getModel(),
                    messages
            ));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getOpenai().getBaseUrl() + "/chat/completions"))
                    .timeout(EXTERNAL_REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI request failed with status " + response.statusCode() + ". Body: " + abbreviate(response.body(), 400)
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            String assistantContent = extractAssistantMessage(root);
            if (!StringUtils.hasText(assistantContent)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include assistant content.");
            }

            return assistantContent.trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI request was interrupted.", exception);
        }
    }

    private String extractAssistantMessage(JsonNode root) {
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return "";
        }

        JsonNode contentNode = choices.get(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }

        return "";
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}
