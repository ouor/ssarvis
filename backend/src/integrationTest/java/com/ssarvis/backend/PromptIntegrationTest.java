package com.ssarvis.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.chat.ChatConversation;
import com.ssarvis.backend.chat.ChatConversationRepository;
import com.ssarvis.backend.chat.ChatMessage;
import com.ssarvis.backend.chat.ChatMessageRepository;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
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
    private ObjectMapper objectMapper;

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
        promptGenerationLogRepository.findAll().stream()
                .filter(log -> log.getCreatedAt() != null && !log.getCreatedAt().isBefore(testStartedAt))
                .map(PromptGenerationLog::getId)
                .forEach(promptGenerationLogRepository::deleteById);
        testStartedAt = Instant.now();
    }

    @Test
    void integrationTestCallsOpenAiAndWritesToMysql() throws Exception {
        long beforeCount = promptGenerationLogRepository.count();
        long beforeConversationCount = chatConversationRepository.count();
        long beforeMessageCount = chatMessageRepository.count();

        String promptResponseBody = mockMvc.perform(post("/api/system-prompt")
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
                .andExpect(jsonPath("$.systemPrompt").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode promptResponse = objectMapper.readTree(promptResponseBody);
        long promptGenerationLogId = promptResponse.get("promptGenerationLogId").asLong();

        String chatResponseBody = mockMvc.perform(post("/api/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promptGenerationLogId": %d,
                                  "message": "내 말투에 맞춰 오늘 일정 정리 방법을 알려줘."
                                }
                                """.formatted(promptGenerationLogId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").isNumber())
                .andExpect(jsonPath("$.assistantMessage").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode chatResponse = objectMapper.readTree(chatResponseBody);
        long conversationId = chatResponse.get("conversationId").asLong();

        long afterCount = promptGenerationLogRepository.count();
        long afterConversationCount = chatConversationRepository.count();
        long afterMessageCount = chatMessageRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount + 1);
        assertThat(afterConversationCount).isEqualTo(beforeConversationCount + 1);
        assertThat(afterMessageCount).isEqualTo(beforeMessageCount + 2);

        PromptGenerationLog latestLog = promptGenerationLogRepository.findAll().stream()
                .max(Comparator.comparing(PromptGenerationLog::getId))
                .orElseThrow();

        assertThat(latestLog.getModel()).isNotBlank();
        assertThat(latestLog.getAnswersJson()).contains("대화할 때 나는 보통");
        assertThat(latestLog.getSystemPrompt()).isNotBlank();

        ChatConversation conversation = chatConversationRepository.findById(conversationId).orElseThrow();
        assertThat(conversation.getPromptGenerationLog().getId()).isEqualTo(promptGenerationLogId);

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByIdAsc(conversationId);
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(ChatMessage.Role.USER);
        assertThat(messages.get(0).getContent()).contains("오늘 일정 정리 방법");
        assertThat(messages.get(1).getRole()).isEqualTo(ChatMessage.Role.ASSISTANT);
        assertThat(messages.get(1).getContent()).isNotBlank();
    }
}
