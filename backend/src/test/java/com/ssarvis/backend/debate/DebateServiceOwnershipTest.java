package com.ssarvis.backend.debate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
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
class DebateServiceOwnershipTest {

    @Mock
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Mock
    private RegisteredVoiceRepository registeredVoiceRepository;

    @Mock
    private DebateSessionRepository debateSessionRepository;

    @Mock
    private DebateTurnRepository debateTurnRepository;

    @Mock
    private VoiceService voiceService;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private AuthService authService;

    private DebateService debateService;

    @BeforeEach
    void setUp() {
        debateService = new DebateService(
                new ObjectMapper(),
                new AppProperties(),
                promptGenerationLogRepository,
                registeredVoiceRepository,
                debateSessionRepository,
                debateTurnRepository,
                voiceService,
                new OpenAiContextAssembler(),
                openAiClient,
                authService
        );
    }

    @Test
    void startDebateRejectsCloneOwnedByAnotherUser() {
        given(authService.getActiveUserAccount(1L)).willReturn(assignId(new UserAccount("haru", "hashed", "하루"), 1L));
        given(promptGenerationLogRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> debateService.startDebate(
                1L,
                new DebateStartRequest(10L, 11L, 20L, 21L, "재택근무가 더 효율적인가?")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    org.assertj.core.api.Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    org.assertj.core.api.Assertions.assertThat(exception.getReason()).isEqualTo("Clone A not found.");
                });
    }

    @Test
    void createNextTurnRejectsDebateSessionOwnedByAnotherUser() {
        given(authService.getActiveUserAccount(1L)).willReturn(assignId(new UserAccount("haru", "hashed", "하루"), 1L));
        given(debateSessionRepository.findByIdAndUserId(77L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> debateService.createNextTurn(1L, 77L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    org.assertj.core.api.Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    org.assertj.core.api.Assertions.assertThat(exception.getReason()).isEqualTo("Debate session not found.");
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
