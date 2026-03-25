package com.ssarvis.backend.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.VoiceService;
import com.ssarvis.backend.voice.VoiceSynthesisResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import com.ssarvis.backend.config.AppProperties;

@Service
public class ChatService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final VoiceService voiceService;

    public ChatService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository,
            ChatConversationRepository chatConversationRepository,
            ChatMessageRepository chatMessageRepository,
            VoiceService voiceService
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.voiceService = voiceService;
    }

    @Transactional
    public ChatResult reply(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message must not be blank.");
        }

        ChatConversation conversation = resolveConversation(request);
        List<ChatMessage> history = conversation.getId() == null
                ? List.of()
                : chatMessageRepository.findByConversationIdOrderByIdAsc(conversation.getId());

        ChatConversation savedConversation = conversation.getId() == null
                ? chatConversationRepository.save(conversation)
                : conversation;

        String userMessage = request.message().trim();
        String assistantMessage = generateAssistantMessage(
                conversation.getPromptGenerationLog().getSystemPrompt(),
                history,
                userMessage
        );

        chatMessageRepository.save(new ChatMessage(savedConversation, ChatMessage.Role.USER, userMessage));

        VoiceSynthesisResult ttsResult = voiceService.synthesize(assistantMessage, request.registeredVoiceId());
        chatMessageRepository.save(new ChatMessage(
                savedConversation,
                ChatMessage.Role.ASSISTANT,
                assistantMessage,
                ttsResult != null ? ttsResult.audioAsset() : null
        ));

        return new ChatResult(
                savedConversation.getId(),
                assistantMessage,
                ttsResult != null ? ttsResult.voiceId() : null,
                ttsResult != null ? ttsResult.audioMimeType() : null,
                ttsResult != null ? ttsResult.audioBase64() : null
        );
    }

    private ChatConversation resolveConversation(ChatRequest request) {
        if (request.conversationId() != null) {
            return chatConversationRepository.findById(request.conversationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found."));
        }

        if (request.promptGenerationLogId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "promptGenerationLogId is required when conversationId is not provided."
            );
        }

        PromptGenerationLog promptGenerationLog = promptGenerationLogRepository.findById(request.promptGenerationLogId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt generation log not found."));

        return new ChatConversation(promptGenerationLog);
    }

    private String generateAssistantMessage(
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage
    ) {
        String apiKey = appProperties.getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_API_KEY is not configured.");
        }

        try {
            String payload = objectMapper.writeValueAsString(buildPayload(systemPrompt, history, userMessage));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getOpenai().getBaseUrl() + "/responses"))
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
            String assistantMessage = extractOutputText(root);
            if (!StringUtils.hasText(assistantMessage)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include output_text.");
            }

            return assistantMessage.trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI request was interrupted.", exception);
        }
    }

    private OpenAiRequest buildPayload(String systemPrompt, List<ChatMessage> history, String userMessage) {
        List<InputMessage> input = new ArrayList<>();
        input.add(new InputMessage("developer", systemPrompt));

        for (ChatMessage message : history) {
            String role = message.getRole() == ChatMessage.Role.USER ? "user" : "assistant";
            input.add(new InputMessage(role, message.getContent()));
        }

        input.add(new InputMessage("user", userMessage));

        return new OpenAiRequest(
                appProperties.getOpenai().getModel(),
                new Reasoning("low"),
                new Text("medium"),
                input
        );
    }

    private String extractOutputText(JsonNode root) {
        JsonNode directOutputText = root.get("output_text");
        if (directOutputText != null && directOutputText.isTextual() && StringUtils.hasText(directOutputText.asText())) {
            return directOutputText.asText();
        }

        JsonNode output = root.get("output");
        if (output == null || !output.isArray()) {
            return "";
        }

        StringBuilder collectedText = new StringBuilder();
        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }

            JsonNode content = item.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }

            for (JsonNode contentItem : content) {
                if (!"output_text".equals(contentItem.path("type").asText())) {
                    continue;
                }

                String text = contentItem.path("text").asText("");
                if (!StringUtils.hasText(text)) {
                    continue;
                }

                if (!collectedText.isEmpty()) {
                    collectedText.append('\n');
                }
                collectedText.append(text);
            }
        }

        return collectedText.toString();
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

    private record OpenAiRequest(
            String model,
            Reasoning reasoning,
            Text text,
            List<InputMessage> input
    ) {
    }

    private record Reasoning(String effort) {
    }

    private record Text(String verbosity) {
    }

    private record InputMessage(String role, String content) {
    }
}
