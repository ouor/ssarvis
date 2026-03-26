package com.ssarvis.backend.chat;

import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.CloneAccessPolicy;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.stream.NdjsonStreamWriter;
import com.ssarvis.backend.voice.VoiceService;
import com.ssarvis.backend.voice.VoiceSynthesisResult;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final VoiceService voiceService;
    private final OpenAiContextAssembler openAiContextAssembler;
    private final OpenAiClient openAiClient;
    private final AuthService authService;
    private final CloneAccessPolicy cloneAccessPolicy;

    public ChatService(
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository,
            ChatConversationRepository chatConversationRepository,
            ChatMessageRepository chatMessageRepository,
            VoiceService voiceService,
            OpenAiContextAssembler openAiContextAssembler,
            OpenAiClient openAiClient,
            AuthService authService,
            CloneAccessPolicy cloneAccessPolicy
    ) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.voiceService = voiceService;
        this.openAiContextAssembler = openAiContextAssembler;
        this.openAiClient = openAiClient;
        this.authService = authService;
        this.cloneAccessPolicy = cloneAccessPolicy;
    }

    @Transactional
    public ChatResult reply(Long userId, ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message must not be blank.");
        }
        UserAccount user = authService.getActiveUserAccount(userId);

        ChatConversation conversation = resolveConversation(user, request);
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

        VoiceSynthesisResult ttsResult = voiceService.synthesize(assistantMessage, request.registeredVoiceId(), userId);
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
    public void streamReply(Long userId, ChatRequest request, OutputStream outputStream) throws IOException {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message must not be blank.");
        }
        UserAccount user = authService.getActiveUserAccount(userId);

        NdjsonStreamWriter writer = new NdjsonStreamWriter(outputStream, objectMapper);
        ChatConversation conversation = resolveConversation(user, request);
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
            ttsResult = voiceService.streamSynthesize(assistantMessage, request.registeredVoiceId(), userId, (base64Chunk, sampleRate, channels) -> writer.write(
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

    @Transactional(readOnly = true)
    public List<ChatConversationSummaryResponse> listConversations(Long userId) {
        authService.getActiveUserAccount(userId);
        return chatConversationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(conversation -> {
                    List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByIdAsc(conversation.getId());
                    String latestMessagePreview = messages.isEmpty()
                            ? ""
                            : abbreviate(messages.get(messages.size() - 1).getContent(), 80);
                    return new ChatConversationSummaryResponse(
                            conversation.getId(),
                            conversation.getPromptGenerationLog().getId(),
                            conversation.getPromptGenerationLog().getAlias(),
                            conversation.getCreatedAt(),
                            latestMessagePreview,
                            messages.size()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatConversationDetailResponse getConversation(Long userId, Long conversationId) {
        authService.getActiveUserAccount(userId);
        ChatConversation conversation = chatConversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found."));

        List<ChatHistoryMessageResponse> messages = chatMessageRepository.findByConversationIdOrderByIdAsc(conversation.getId()).stream()
                .map(message -> new ChatHistoryMessageResponse(
                        message.getRole().name().toLowerCase(),
                        message.getContent(),
                        message.getCreatedAt(),
                        message.getAudioAsset() != null ? message.getAudioAsset().getObjectUrl() : null,
                        message.getAudioAsset() != null ? message.getAudioAsset().getProviderVoiceId() : null
                ))
                .toList();

        return new ChatConversationDetailResponse(
                conversation.getId(),
                conversation.getPromptGenerationLog().getId(),
                conversation.getPromptGenerationLog().getAlias(),
                conversation.getPromptGenerationLog().getShortDescription(),
                conversation.getCreatedAt(),
                messages
        );
    }

    private ChatConversation resolveConversation(UserAccount user, ChatRequest request) {
        if (request.conversationId() != null) {
            return chatConversationRepository.findByIdAndUserId(request.conversationId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found."));
        }

        if (request.promptGenerationLogId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "promptGenerationLogId is required when conversationId is not provided."
            );
        }

        PromptGenerationLog promptGenerationLog = cloneAccessPolicy.getUsableClone(user.getId(), request.promptGenerationLogId());

        return new ChatConversation(user, promptGenerationLog);
    }

    private String generateAssistantMessage(String systemPrompt, List<ChatMessage> history, String userMessage) {
        return openAiClient.requestChatCompletion(
                openAiContextAssembler.buildChatMessages(
                        systemPrompt,
                        history,
                        userMessage,
                        appProperties.getOpenai().getChatHistoryTurns()
                )
        );
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
