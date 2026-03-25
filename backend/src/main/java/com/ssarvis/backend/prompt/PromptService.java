package com.ssarvis.backend.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiChatCompletionRequest;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromptService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final OpenAiContextAssembler openAiContextAssembler;

    public PromptService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository,
            OpenAiContextAssembler openAiContextAssembler
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.openAiContextAssembler = openAiContextAssembler;
    }

    public PromptGenerateResult generateSystemPrompt(PromptGenerateRequest request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one answer is required.");
        }

        String apiKey = appProperties.getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "OPENAI_API_KEY is not configured."
            );
        }

        try {
            String payload = objectMapper.writeValueAsString(buildPayload(request.answers()));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getOpenai().getBaseUrl() + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                String responsePreview = abbreviate(response.body(), 400);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI request failed with status " + response.statusCode() + ". Body: " + responsePreview
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            String systemPrompt = extractAssistantMessage(root);
            if (!StringUtils.hasText(systemPrompt)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI response did not include assistant content."
                );
            }

            String trimmedPrompt = systemPrompt.trim();
            PromptGenerationLog log = saveGenerationLog(request.answers(), trimmedPrompt);
            return new PromptGenerateResult(log.getId(), trimmedPrompt);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI request was interrupted.", exception);
        }
    }

    private PromptGenerationLog saveGenerationLog(List<PromptGenerateRequest.AnswerItem> answers, String systemPrompt) throws IOException {
        String answersJson = objectMapper.writeValueAsString(answers);
        PromptGenerationLog log = new PromptGenerationLog(
                appProperties.getOpenai().getModel(),
                answersJson,
                systemPrompt
        );
        return promptGenerationLogRepository.save(log);
    }

    private OpenAiChatCompletionRequest buildPayload(List<PromptGenerateRequest.AnswerItem> answers) {
        return new OpenAiChatCompletionRequest(
                appProperties.getOpenai().getModel(),
                openAiContextAssembler.buildPromptGenerationMessages(answers)
        );
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
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
