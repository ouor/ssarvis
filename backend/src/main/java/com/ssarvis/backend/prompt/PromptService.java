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
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromptService {
    private static final Duration EXTERNAL_REQUEST_TIMEOUT = Duration.ofSeconds(20);

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
            String systemPrompt = normalizeSystemPrompt(requestText(buildSystemPromptPayload(request.answers())));
            if (!StringUtils.hasText(systemPrompt)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include system prompt content.");
            }

            String alias = normalizeAlias(requestText(buildAliasPayload(systemPrompt)));
            if (!StringUtils.hasText(alias)) {
                alias = "새 클론";
            }

            String shortDescription = normalizeShortDescription(requestText(buildShortDescriptionPayload(systemPrompt)));
            if (!StringUtils.hasText(shortDescription)) {
                shortDescription = abbreviate(systemPrompt.replaceAll("\\s+", " ").trim(), 60);
            }

            GeneratedCloneProfile profile = new GeneratedCloneProfile(alias, shortDescription, systemPrompt);
            PromptGenerationLog log = saveGenerationLog(request.answers(), profile);
            return new PromptGenerateResult(log.getId(), log.getAlias(), log.getShortDescription(), log.getSystemPrompt());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI request was interrupted.", exception);
        }
    }

    private PromptGenerationLog saveGenerationLog(List<PromptGenerateRequest.AnswerItem> answers, GeneratedCloneProfile profile) throws IOException {
        String answersJson = objectMapper.writeValueAsString(answers);
        PromptGenerationLog log = new PromptGenerationLog(
                appProperties.getOpenai().getModel(),
                answersJson,
                profile.systemPrompt(),
                profile.alias(),
                profile.shortDescription()
        );
        return promptGenerationLogRepository.save(log);
    }

    private OpenAiChatCompletionRequest buildSystemPromptPayload(List<PromptGenerateRequest.AnswerItem> answers) {
        return new OpenAiChatCompletionRequest(
                appProperties.getOpenai().getModel(),
                openAiContextAssembler.buildSystemPromptGenerationMessages(answers)
        );
    }

    private OpenAiChatCompletionRequest buildAliasPayload(String systemPrompt) {
        return new OpenAiChatCompletionRequest(
                appProperties.getOpenai().getModel(),
                openAiContextAssembler.buildAliasGenerationMessages(systemPrompt)
        );
    }

    private OpenAiChatCompletionRequest buildShortDescriptionPayload(String systemPrompt) {
        return new OpenAiChatCompletionRequest(
                appProperties.getOpenai().getModel(),
                openAiContextAssembler.buildShortDescriptionGenerationMessages(systemPrompt)
        );
    }

    private String requestText(OpenAiChatCompletionRequest requestPayload) throws IOException, InterruptedException {
        String payload = objectMapper.writeValueAsString(requestPayload);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(appProperties.getOpenai().getBaseUrl() + "/chat/completions"))
                .timeout(EXTERNAL_REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + appProperties.getOpenai().getApiKey())
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
        String assistantContent = extractAssistantMessage(root);
        if (!StringUtils.hasText(assistantContent)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include assistant content.");
        }
        return assistantContent.trim();
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

    private String normalizeSystemPrompt(String systemPrompt) {
        return systemPrompt == null ? "" : systemPrompt.trim();
    }

    private String normalizeAlias(String alias) {
        String normalized = Normalizer.normalize(alias == null ? "" : alias, Normalizer.Form.NFC).trim();
        normalized = normalized.replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private String normalizeShortDescription(String shortDescription) {
        String normalized = (shortDescription == null ? "" : shortDescription).replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private record GeneratedCloneProfile(String alias, String shortDescription, String systemPrompt) {
    }
}
