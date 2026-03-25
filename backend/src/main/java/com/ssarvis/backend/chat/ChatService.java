package com.ssarvis.backend.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiChatCompletionRequest;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.stream.NdjsonStreamWriter;
import com.ssarvis.backend.voice.VoiceService;
import com.ssarvis.backend.voice.VoiceSynthesisResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final VoiceService voiceService;
    private final OpenAiContextAssembler openAiContextAssembler;

    public ChatService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository,
            ChatConversationRepository chatConversationRepository,
            ChatMessageRepository chatMessageRepository,
            VoiceService voiceService,
            OpenAiContextAssembler openAiContextAssembler
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.voiceService = voiceService;
        this.openAiContextAssembler = openAiContextAssembler;
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

    @Transactional
    public void streamReply(ChatRequest request, OutputStream outputStream) throws IOException {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message must not be blank.");
        }

        NdjsonStreamWriter writer = new NdjsonStreamWriter(outputStream, objectMapper);
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
        writer.write(Map.of(
                "type", "message",
                "conversationId", savedConversation.getId(),
                "assistantMessage", assistantMessage
        ));

        VoiceSynthesisResult ttsResult = null;
        try {
            ttsResult = voiceService.streamSynthesize(assistantMessage, request.registeredVoiceId(), (base64Chunk, sampleRate, channels) -> writer.write(
                    Map.of(
                            "type", "audio_chunk",
                            "audioFormat", "pcm_s16le",
                            "sampleRate", sampleRate,
                            "channels", channels,
                            "chunkBase64", base64Chunk
                    )
            ));
        } catch (ResponseStatusException exception) {
            writer.writeError(exception.getReason() != null ? exception.getReason() : "Failed to stream TTS audio.");
        } catch (Exception exception) {
            writer.writeError("Failed to stream TTS audio.");
        }

        chatMessageRepository.save(new ChatMessage(
                savedConversation,
                ChatMessage.Role.ASSISTANT,
                assistantMessage,
                ttsResult != null ? ttsResult.audioAsset() : null
        ));

        writer.write(Map.of(
                "type", "done",
                "conversationId", savedConversation.getId(),
                "ttsVoiceId", ttsResult != null ? ttsResult.voiceId() : "",
                "hasAudio", ttsResult != null
        ));
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

    private String generateAssistantMessage(String systemPrompt, List<ChatMessage> history, String userMessage) {
        String apiKey = appProperties.getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_API_KEY is not configured.");
        }

        try {
            String payload = objectMapper.writeValueAsString(buildPayload(systemPrompt, history, userMessage));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getOpenai().getBaseUrl() + "/chat/completions"))
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
            String assistantMessage = extractAssistantMessage(root);
            if (!StringUtils.hasText(assistantMessage)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include assistant content.");
            }

            return assistantMessage.trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI request was interrupted.", exception);
        }
    }

    private OpenAiChatCompletionRequest buildPayload(String systemPrompt, List<ChatMessage> history, String userMessage) {
        return new OpenAiChatCompletionRequest(
                appProperties.getOpenai().getModel(),
                openAiContextAssembler.buildChatMessages(
                        systemPrompt,
                        history,
                        userMessage,
                        appProperties.getOpenai().getChatHistoryTurns()
                )
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
