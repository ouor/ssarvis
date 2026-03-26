package com.ssarvis.backend.debate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.CloneAccessPolicy;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
import com.ssarvis.backend.voice.RegisteredVoice;
import com.ssarvis.backend.voice.VoiceAccessPolicy;
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

    @Mock
    private VoiceAccessPolicy voiceAccessPolicy;

    @Mock
    private CloneAccessPolicy cloneAccessPolicy;

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
                voiceAccessPolicy,
                new OpenAiContextAssembler(),
                openAiClient,
                authService,
                cloneAccessPolicy
        );
    }

    @Test
    void startDebateRejectsCloneOwnedByAnotherUser() {
        given(authService.getActiveUserAccount(1L)).willReturn(assignId(new UserAccount("haru", "hashed", "하루"), 1L));
        given(cloneAccessPolicy.getUsableClone(1L, 10L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt generation log not found."));

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
    void startDebateAllowsPublicClonesAndVoicesOwnedByAnotherUser() {
        UserAccount requester = assignId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount owner = assignId(new UserAccount("miso", "hashed", "미소"), 2L);
        PromptGenerationLog cloneA = assignCloneId(new PromptGenerationLog(owner, "gpt-5", "[]", "a-system", "공개 A", "설명 A"), 10L);
        PromptGenerationLog cloneB = assignCloneId(new PromptGenerationLog(owner, "gpt-5", "[]", "b-system", "공개 B", "설명 B"), 11L);
        cloneA.updateVisibility(true);
        cloneB.updateVisibility(true);
        RegisteredVoice voiceA = assignVoiceId(new RegisteredVoice(owner, "voice-a", "tts-model", "voicea", "공개 음성 A", "a.wav", "audio/wav"), 20L);
        RegisteredVoice voiceB = assignVoiceId(new RegisteredVoice(owner, "voice-b", "tts-model", "voiceb", "공개 음성 B", "b.wav", "audio/wav"), 21L);
        voiceA.updateVisibility(true);
        voiceB.updateVisibility(true);
        AppProperties appProperties = new AppProperties();
        appProperties.getDashscope().setTtsModel("tts-model");
        debateService = new DebateService(
                new ObjectMapper(),
                appProperties,
                promptGenerationLogRepository,
                registeredVoiceRepository,
                debateSessionRepository,
                debateTurnRepository,
                voiceService,
                voiceAccessPolicy,
                new OpenAiContextAssembler(),
                openAiClient,
                authService,
                cloneAccessPolicy
        );

        given(authService.getActiveUserAccount(1L)).willReturn(requester);
        given(cloneAccessPolicy.getUsableClone(1L, 10L)).willReturn(cloneA);
        given(cloneAccessPolicy.getUsableClone(1L, 11L)).willReturn(cloneB);
        given(voiceAccessPolicy.getUsableVoice(1L, 20L)).willReturn(voiceA);
        given(voiceAccessPolicy.getUsableVoice(1L, 21L)).willReturn(voiceB);
        given(debateSessionRepository.save(any())).willAnswer(invocation -> assignDebateSessionId(invocation.getArgument(0), 55L));
        given(debateTurnRepository.findByDebateSessionIdOrderByTurnIndexAsc(55L)).willReturn(List.of());
        given(openAiClient.requestChatCompletion(any())).willReturn("첫 공개 발언");
        given(voiceService.synthesize("첫 공개 발언", 20L, 1L)).willReturn(null);

        debateService.startDebate(1L, new DebateStartRequest(10L, 11L, 20L, 21L, "주제"));
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

    private PromptGenerationLog assignCloneId(PromptGenerationLog clone, Long id) {
        try {
            java.lang.reflect.Field idField = PromptGenerationLog.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(clone, id);
            return clone;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign clone id.", exception);
        }
    }

    private RegisteredVoice assignVoiceId(RegisteredVoice voice, Long id) {
        try {
            java.lang.reflect.Field idField = RegisteredVoice.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(voice, id);
            return voice;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign voice id.", exception);
        }
    }

    private DebateSession assignDebateSessionId(DebateSession debateSession, Long id) {
        try {
            java.lang.reflect.Field idField = DebateSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(debateSession, id);
            return debateSession;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign debate session id.", exception);
        }
    }
}
