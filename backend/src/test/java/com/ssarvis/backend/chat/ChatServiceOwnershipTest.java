package com.ssarvis.backend.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.VoiceService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ChatServiceOwnershipTest {

    @Mock
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Mock
    private ChatConversationRepository chatConversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private VoiceService voiceService;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private AuthService authService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getOpenai().setChatHistoryTurns(10);
        chatService = new ChatService(
                new ObjectMapper(),
                appProperties,
                promptGenerationLogRepository,
                chatConversationRepository,
                chatMessageRepository,
                voiceService,
                new OpenAiContextAssembler(),
                openAiClient,
                authService
        );
    }

    @Test
    void replyRejectsPromptGenerationLogOwnedByAnotherUser() {
        given(authService.getActiveUserAccount(1L)).willReturn(assignId(new UserAccount("haru", "hashed", "하루"), 1L));
        given(promptGenerationLogRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.reply(1L, new ChatRequest(99L, null, null, "안녕")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    org.assertj.core.api.Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    org.assertj.core.api.Assertions.assertThat(exception.getReason()).isEqualTo("Prompt generation log not found.");
                });
    }

    @Test
    void replyRejectsConversationOwnedByAnotherUser() {
        given(authService.getActiveUserAccount(1L)).willReturn(assignId(new UserAccount("haru", "hashed", "하루"), 1L));
        given(chatConversationRepository.findByIdAndUserId(88L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.reply(1L, new ChatRequest(null, 88L, null, "안녕")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    org.assertj.core.api.Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    org.assertj.core.api.Assertions.assertThat(exception.getReason()).isEqualTo("Conversation not found.");
                });
    }

    private UserAccount assignId(UserAccount userAccount, Long id) {
        try {
            java.lang.reflect.Field idField = UserAccount.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(userAccount, id);
            return userAccount;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }
}
