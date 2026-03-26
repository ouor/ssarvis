package com.ssarvis.backend.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.CloneAccessPolicy;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.VoiceService;
import java.util.Optional;
import java.util.List;
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

    @Mock
    private CloneAccessPolicy cloneAccessPolicy;

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
                authService,
                cloneAccessPolicy
        );
    }

    @Test
    void replyRejectsPromptGenerationLogOwnedByAnotherUser() {
        given(authService.getActiveUserAccount(1L)).willReturn(assignId(new UserAccount("haru", "hashed", "하루"), 1L));
        given(cloneAccessPolicy.getUsableClone(1L, 99L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt generation log not found."));

        assertThatThrownBy(() -> chatService.reply(1L, new ChatRequest(99L, null, null, "안녕")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    org.assertj.core.api.Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    org.assertj.core.api.Assertions.assertThat(exception.getReason()).isEqualTo("Prompt generation log not found.");
                });
    }

    @Test
    void replyAllowsPublicPromptGenerationLogOwnedByAnotherUser() {
        UserAccount user = assignId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount owner = assignId(new UserAccount("miso", "hashed", "미소"), 2L);
        PromptGenerationLog publicClone = new PromptGenerationLog(owner, "gpt-5", "[]", "public-system-prompt", "공개 클론", "공개 설명");
        publicClone.updateVisibility(true);

        given(authService.getActiveUserAccount(1L)).willReturn(user);
        given(cloneAccessPolicy.getUsableClone(1L, 99L)).willReturn(publicClone);
        given(chatConversationRepository.save(any())).willAnswer(invocation -> {
            ChatConversation conversation = invocation.getArgument(0);
            java.lang.reflect.Field idField = ChatConversation.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(conversation, 31L);
            return conversation;
        });
        given(openAiClient.requestChatCompletion(any())).willReturn("공개 클론 응답");
        chatService.reply(1L, new ChatRequest(99L, null, null, "안녕"));
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
