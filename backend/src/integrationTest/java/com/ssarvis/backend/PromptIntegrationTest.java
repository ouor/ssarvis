package com.ssarvis.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.AuthResponse;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.auth.UserAccountRepository;
import com.ssarvis.backend.chat.ChatConversation;
import com.ssarvis.backend.chat.ChatConversationRepository;
import com.ssarvis.backend.chat.ChatMessage;
import com.ssarvis.backend.chat.ChatMessageRepository;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.debate.DebateSession;
import com.ssarvis.backend.debate.DebateSessionRepository;
import com.ssarvis.backend.debate.DebateTurn;
import com.ssarvis.backend.debate.DebateTurnRepository;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.GeneratedAudioAsset;
import com.ssarvis.backend.voice.GeneratedAudioAssetRepository;
import com.ssarvis.backend.voice.RegisteredVoice;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestPropertySource(properties = {
        "app.auth.jwt.secret=integration-test-jwt-secret-key-that-is-at-least-32-bytes-long"
})
class PromptIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Autowired
    private ChatConversationRepository chatConversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private RegisteredVoiceRepository registeredVoiceRepository;

    @Autowired
    private GeneratedAudioAssetRepository generatedAudioAssetRepository;

    @Autowired
    private DebateSessionRepository debateSessionRepository;

    @Autowired
    private DebateTurnRepository debateTurnRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AppProperties appProperties;

    private Instant testStartedAt = Instant.now();

    @AfterEach
    void cleanUpCreatedRows() {
        chatMessageRepository.findAll().stream()
                .filter(message -> message.getCreatedAt() != null && !message.getCreatedAt().isBefore(testStartedAt))
                .map(ChatMessage::getId)
                .forEach(chatMessageRepository::deleteById);
        chatConversationRepository.findAll().stream()
                .filter(conversation -> conversation.getCreatedAt() != null && !conversation.getCreatedAt().isBefore(testStartedAt))
                .map(ChatConversation::getId)
                .forEach(chatConversationRepository::deleteById);
        debateTurnRepository.findAll().stream()
                .filter(turn -> turn.getCreatedAt() != null && !turn.getCreatedAt().isBefore(testStartedAt))
                .map(DebateTurn::getId)
                .forEach(debateTurnRepository::deleteById);
        debateSessionRepository.findAll().stream()
                .filter(session -> session.getCreatedAt() != null && !session.getCreatedAt().isBefore(testStartedAt))
                .map(DebateSession::getId)
                .forEach(debateSessionRepository::deleteById);
        registeredVoiceRepository.findAll().stream()
                .filter(voice -> voice.getCreatedAt() != null && !voice.getCreatedAt().isBefore(testStartedAt))
                .map(RegisteredVoice::getId)
                .forEach(registeredVoiceRepository::deleteById);
        generatedAudioAssetRepository.findAll().stream()
                .filter(asset -> asset.getCreatedAt() != null && !asset.getCreatedAt().isBefore(testStartedAt))
                .map(GeneratedAudioAsset::getId)
                .forEach(generatedAudioAssetRepository::deleteById);
        promptGenerationLogRepository.findAll().stream()
                .filter(log -> log.getCreatedAt() != null && !log.getCreatedAt().isBefore(testStartedAt))
                .map(PromptGenerationLog::getId)
                .forEach(promptGenerationLogRepository::deleteById);
        userAccountRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null && !user.getCreatedAt().isBefore(testStartedAt))
                .map(UserAccount::getId)
                .forEach(userAccountRepository::deleteById);
        testStartedAt = Instant.now();
    }

    @Test
    void integrationTestCallsProtectedApisWithJwtAndWritesOwnedRows() throws Exception {
        long beforeCount = promptGenerationLogRepository.count();
        long beforeConversationCount = chatConversationRepository.count();
        long beforeMessageCount = chatMessageRepository.count();
        long beforeRegisteredVoiceCount = registeredVoiceRepository.count();
        long beforeAudioAssetCount = generatedAudioAssetRepository.count();

        AuthResponse auth = signUp("integration-user-" + System.currentTimeMillis(), "secret123", "통합테스터");
        String authorizationHeader = bearer(auth.accessToken());

        String promptResponseBody = mockMvc.perform(post("/api/system-prompt")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "question": "대화할 때 나는 보통",
                                      "answer": "상대가 걸어오면 잘 받는 편"
                                    },
                                    {
                                      "question": "평소 결정은 어떤 편인가요?",
                                      "answer": "충분히 고민하고 정함"
                                    },
                                    {
                                      "question": "좋아하는 분위기에 가까운 것은?",
                                      "answer": "조용하고 차분한 분위기"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptGenerationLogId").isNumber())
                .andExpect(jsonPath("$.alias").isString())
                .andExpect(jsonPath("$.shortDescription").isString())
                .andExpect(jsonPath("$.systemPrompt").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode promptResponse = objectMapper.readTree(promptResponseBody);
        long promptGenerationLogId = promptResponse.get("promptGenerationLogId").asLong();

        PromptGenerationLog firstPromptLog = promptGenerationLogRepository.findById(promptGenerationLogId).orElseThrow();
        UserAccount owner = userAccountRepository.findById(auth.userId()).orElseThrow();
        PromptGenerationLog secondPromptLog = promptGenerationLogRepository.save(new PromptGenerationLog(
                owner,
                firstPromptLog.getModel(),
                firstPromptLog.getAnswersJson(),
                firstPromptLog.getSystemPrompt(),
                abbreviateAlias(firstPromptLog.getAlias()),
                firstPromptLog.getShortDescription()
        ));
        long secondPromptGenerationLogId = secondPromptLog.getId();

        SampleAudio dashScopeSampleAudio = createDashScopeSampleAudio();
        String voiceResponseBody = mockMvc.perform(multipart("/api/voices")
                        .file(new MockMultipartFile(
                                "sample",
                                "dashscope-sample",
                                dashScopeSampleAudio.contentType(),
                                dashScopeSampleAudio.bytes()
                        ))
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredVoiceId").isNumber())
                .andExpect(jsonPath("$.voiceId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode voiceResponse = objectMapper.readTree(voiceResponseBody);
        long registeredVoiceId = voiceResponse.get("registeredVoiceId").asLong();

        String chatResponseBody = mockMvc.perform(post("/api/chat/messages")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promptGenerationLogId": %d,
                                  "registeredVoiceId": %d,
                                  "message": "오늘 일정 정리 팁 알려줘."
                                }
                                """.formatted(promptGenerationLogId, registeredVoiceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").isNumber())
                .andExpect(jsonPath("$.assistantMessage").isString())
                .andExpect(jsonPath("$.ttsVoiceId").isString())
                .andExpect(jsonPath("$.ttsAudioMimeType").isString())
                .andExpect(jsonPath("$.ttsAudioBase64").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode chatResponse = objectMapper.readTree(chatResponseBody);
        long conversationId = chatResponse.get("conversationId").asLong();

        String debateResponseBody = mockMvc.perform(post("/api/debates")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cloneAId": %d,
                                  "cloneBId": %d,
                                  "cloneAVoiceId": %d,
                                  "cloneBVoiceId": %d,
                                  "topic": "재택근무가 더 효율적인가?"
                                }
                                """.formatted(
                                promptGenerationLogId,
                                secondPromptGenerationLogId,
                                registeredVoiceId,
                                registeredVoiceId
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debateSessionId").isNumber())
                .andExpect(jsonPath("$.turn.speaker").value("CLONE_A"))
                .andExpect(jsonPath("$.turn.ttsVoiceId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode debateResponse = objectMapper.readTree(debateResponseBody);
        long debateSessionId = debateResponse.get("debateSessionId").asLong();

        long afterCount = promptGenerationLogRepository.count();
        long afterConversationCount = chatConversationRepository.count();
        long afterMessageCount = chatMessageRepository.count();
        long afterRegisteredVoiceCount = registeredVoiceRepository.count();
        long afterAudioAssetCount = generatedAudioAssetRepository.count();
        long debateSessionCount = debateSessionRepository.count();
        long debateTurnCount = debateTurnRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount + 2);
        assertThat(afterConversationCount).isEqualTo(beforeConversationCount + 1);
        assertThat(afterMessageCount).isEqualTo(beforeMessageCount + 2);
        assertThat(afterRegisteredVoiceCount).isEqualTo(beforeRegisteredVoiceCount + 1);
        if (appProperties.getStorage().getS3().isEnabled()) {
            assertThat(afterAudioAssetCount).isEqualTo(beforeAudioAssetCount + 2);
        } else {
            assertThat(afterAudioAssetCount).isEqualTo(beforeAudioAssetCount);
        }
        assertThat(debateSessionCount).isGreaterThanOrEqualTo(1);
        assertThat(debateTurnCount).isGreaterThanOrEqualTo(1);

        PromptGenerationLog latestLog = promptGenerationLogRepository.findAll().stream()
                .max(Comparator.comparing(PromptGenerationLog::getId))
                .orElseThrow();

        assertThat(latestLog.getModel()).isNotBlank();
        assertThat(latestLog.getAnswersJson()).contains("대화할 때 나는 보통");
        assertThat(latestLog.getAlias()).isNotBlank();
        assertThat(latestLog.getShortDescription()).isNotBlank();
        assertThat(latestLog.getSystemPrompt()).isNotBlank();

        ChatConversation conversation = chatConversationRepository.findById(conversationId).orElseThrow();
        assertThat(conversation.getUser().getId()).isEqualTo(auth.userId());
        assertThat(conversation.getPromptGenerationLog().getId()).isEqualTo(promptGenerationLogId);

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByIdAsc(conversationId);
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(ChatMessage.Role.USER);
        assertThat(messages.get(0).getContent()).contains("오늘 일정 정리 팁");
        assertThat(messages.get(1).getRole()).isEqualTo(ChatMessage.Role.ASSISTANT);
        assertThat(messages.get(1).getContent()).isNotBlank();
        if (appProperties.getStorage().getS3().isEnabled()) {
            List<GeneratedAudioAsset> generatedAudioAssets = generatedAudioAssetRepository.findAll();
            assertThat(generatedAudioAssets).hasSize((int) afterAudioAssetCount);
            assertThat(generatedAudioAssets).allSatisfy(asset -> {
                assertThat(asset.getObjectKey()).endsWith(".mp3");
                assertThat(asset.getObjectUrl()).isNotBlank();
                assertThat(asset.getStoredAudioMimeType()).isEqualTo("audio/mpeg");
            });
        }
        assertThat(chatResponse.get("ttsVoiceId").asText()).isNotBlank();
        assertThat(chatResponse.get("ttsAudioMimeType").asText()).startsWith("audio/");
        assertThat(chatResponse.get("ttsAudioBase64").asText()).isNotBlank();

        DebateSession debateSession = debateSessionRepository.findById(debateSessionId).orElseThrow();
        assertThat(debateSession.getUser().getId()).isEqualTo(auth.userId());
        assertThat(debateSession.getCloneA().getId()).isEqualTo(promptGenerationLogId);
        assertThat(debateSession.getCloneB().getId()).isEqualTo(secondPromptGenerationLogId);

        List<DebateTurn> debateTurns = debateTurnRepository.findByDebateSessionIdOrderByTurnIndexAsc(debateSessionId);
        assertThat(debateTurns).hasSize(1);
        assertThat(debateTurns.get(0).getSpeaker()).isEqualTo(DebateTurn.Speaker.CLONE_A);
        assertThat(debateTurns.get(0).getContent()).isNotBlank();

        mockMvc.perform(get("/api/clones")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cloneId").isNumber());
    }

    @Test
    void softDeletedUserTokenStopsWorkingInIntegrationFlow() throws Exception {
        AuthResponse auth = signUp("soft-delete-user-" + System.currentTimeMillis(), "secret123", "삭제테스터");
        String authorizationHeader = bearer(auth.accessToken());

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(auth.userId()));

        mockMvc.perform(delete("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/clones")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User not found or inactive."));
    }

    private SampleAudio createDashScopeSampleAudio() throws Exception {
        try {
            Path samplePath = Path.of("src", "test", "resources", "sample", "haru.wav");
            return new SampleAudio(Files.readAllBytes(samplePath), "audio/wav");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load sample voice file from test resources.", exception);
        }
    }

    private String abbreviateAlias(String alias) {
        String baseAlias = (alias == null ? "clone" : alias).trim();
        if (baseAlias.length() >= 118) {
            return baseAlias.substring(0, 118) + " 2";
        }
        return baseAlias + " 2";
    }

    private AuthResponse signUp(String username, String password, String displayName) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(username, password, displayName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, AuthResponse.class);
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record SampleAudio(byte[] bytes, String contentType) {
    }
}
