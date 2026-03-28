package com.ssarvis.backend.dm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.AutoReplyMode;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.follow.FollowRepository;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.VoiceService;
import com.ssarvis.backend.voice.VoiceSynthesisResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DmServiceTest {

    @Mock
    private DmThreadRepository dmThreadRepository;

    @Mock
    private DmMessageRepository dmMessageRepository;

    @Mock
    private AuthService authService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private DmHiddenBundleRepository dmHiddenBundleRepository;

    @Mock
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Mock
    private OpenAiContextAssembler openAiContextAssembler;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private VoiceService voiceService;

    private DmService dmService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getOpenai().setChatHistoryTurns(10);
        dmService = new DmService(
                dmThreadRepository,
                dmMessageRepository,
                dmHiddenBundleRepository,
                authService,
                followRepository,
                promptGenerationLogRepository,
                openAiContextAssembler,
                openAiClient,
                appProperties,
                voiceService
        );
    }

    @Test
    void startThreadCreatesThreadForPublicAccount() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(authService.getActiveUserAccount(2L)).willReturn(target);
        given(dmThreadRepository.findByParticipants(1L, 2L)).willReturn(Optional.empty());
        given(dmThreadRepository.save(any())).willAnswer(invocation -> {
            DmThread thread = invocation.getArgument(0);
            reflectId(thread, 11L);
            reflectCreatedAt(thread, Instant.parse("2026-03-28T00:00:00Z"));
            return thread;
        });

        DmThreadDetailResponse response = dmService.startThread(1L, new DmStartRequest(2L));

        assertThat(response.threadId()).isEqualTo(11L);
        assertThat(response.otherParticipant().userId()).isEqualTo(2L);
        assertThat(response.messages()).isEmpty();
        assertThat(response.hiddenBundleMessageIds()).isEmpty();
    }

    @Test
    void startThreadRejectsUnfollowedPrivateAccount() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        target.updateVisibility(AccountVisibility.PRIVATE);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(authService.getActiveUserAccount(2L)).willReturn(target);
        given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> dmService.startThread(1L, new DmStartRequest(2L)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("This private account is not available for DM.");
                });
    }

    @Test
    void sendMessagePersistsForParticipant() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        DmThread thread = reflectId(new DmThread(me, target), 21L);
        reflectCreatedAt(thread, Instant.parse("2026-03-28T00:00:00Z"));
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(dmThreadRepository.findWithParticipantsById(21L)).willReturn(Optional.of(thread));
        given(dmMessageRepository.save(any())).willAnswer(invocation -> {
            DmMessage message = invocation.getArgument(0);
            reflectId(message, 31L);
            reflectCreatedAt(message, Instant.parse("2026-03-28T00:01:00Z"));
            return message;
        });

        DmMessageResponse response = dmService.sendMessage(1L, 21L, new DmSendMessageRequest("안녕!"));

        assertThat(response.messageId()).isEqualTo(31L);
        assertThat(response.senderUserId()).isEqualTo(1L);
        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.bundleRootMessageId()).isNull();
        assertThat(response.content()).isEqualTo("안녕!");
    }

    @Test
    void sendMessageCreatesAiProxyReplyWhenRecipientIsAway() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        target.updateAutoReplyMode(AutoReplyMode.AWAY);
        reflectLastActivityAt(target, Instant.now().minusSeconds(240));

        DmThread thread = reflectId(new DmThread(me, target), 21L);
        reflectCreatedAt(thread, Instant.parse("2026-03-28T00:00:00Z"));
        PromptGenerationLog clone = reflectId(new PromptGenerationLog(target, "gpt-5", "[]", "system prompt", "미소", "설명"), 91L);

        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(dmThreadRepository.findWithParticipantsById(21L)).willReturn(Optional.of(thread));
        given(dmMessageRepository.save(any())).willAnswer(invocation -> {
            DmMessage message = invocation.getArgument(0);
            reflectId(message, message.getKind() == DmMessageKind.AI_PROXY ? 32L : 31L);
            reflectCreatedAt(message, Instant.parse("2026-03-28T00:01:00Z"));
            return message;
        });
        given(promptGenerationLogRepository.findTopByUserIdOrderByIdDesc(2L)).willReturn(Optional.of(clone));
        given(dmMessageRepository.findByThreadIdOrderByCreatedAtAsc(21L)).willReturn(List.of(
                reflectId(new DmMessage(thread, me, "안녕!"), 31L)
        ));
        given(openAiContextAssembler.buildDmAutoReplyMessages(any(), any(), any(), any(Integer.class))).willReturn(List.of());
        given(openAiClient.requestChatCompletion(any())).willReturn("지금은 잠깐 자리를 비웠어요.");

        DmMessageResponse response = dmService.sendMessage(1L, 21L, new DmSendMessageRequest("안녕!"));

        assertThat(response.aiGenerated()).isFalse();
        verify(dmMessageRepository, times(2)).save(any());
    }

    @Test
    void sendMessageSkipsAiProxyReplyWhenRecipientAutoReplyIsOff() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        target.updateAutoReplyMode(AutoReplyMode.OFF);
        DmThread thread = reflectId(new DmThread(me, target), 21L);
        reflectCreatedAt(thread, Instant.parse("2026-03-28T00:00:00Z"));

        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(dmThreadRepository.findWithParticipantsById(21L)).willReturn(Optional.of(thread));
        given(dmMessageRepository.save(any())).willAnswer(invocation -> {
            DmMessage message = invocation.getArgument(0);
            reflectId(message, 31L);
            reflectCreatedAt(message, Instant.parse("2026-03-28T00:01:00Z"));
            return message;
        });

        dmService.sendMessage(1L, 21L, new DmSendMessageRequest("안녕!"));

        verify(dmMessageRepository, times(1)).save(any());
    }

    @Test
    void listThreadsReturnsSummariesWithLatestPreview() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        DmThread thread = reflectId(new DmThread(me, target), 41L);
        reflectCreatedAt(thread, Instant.parse("2026-03-28T00:00:00Z"));
        DmMessage latestMessage = reflectId(new DmMessage(thread, target, "최근 메시지"), 51L);
        reflectCreatedAt(latestMessage, Instant.parse("2026-03-28T00:02:00Z"));
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(dmThreadRepository.findAllByParticipantIdOrderByCreatedAtDesc(1L)).willReturn(List.of(thread));
        given(dmMessageRepository.findByThreadIdOrderByCreatedAtDesc(41L)).willReturn(List.of(latestMessage));

        List<DmThreadSummaryResponse> results = dmService.listThreads(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).latestMessagePreview()).isEqualTo("최근 메시지");
    }

    @Test
    void hideBundleStoresViewerScopedVisibility() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        DmThread thread = reflectId(new DmThread(me, target), 41L);
        DmMessage humanMessage = reflectId(new DmMessage(thread, me, "AI를 부른 메시지"), 51L);

        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(dmThreadRepository.findWithParticipantsById(41L)).willReturn(Optional.of(thread));
        given(dmMessageRepository.findByIdAndThreadId(51L, 41L)).willReturn(Optional.of(humanMessage));
        given(dmMessageRepository.existsByThreadIdAndKindAndTriggerMessageId(41L, DmMessageKind.AI_PROXY, 51L)).willReturn(true);
        given(dmHiddenBundleRepository.existsByViewerIdAndBundleRootMessageId(1L, 51L)).willReturn(false);

        DmBundleVisibilityResponse response = dmService.hideBundle(1L, 41L, 51L);

        assertThat(response.bundleRootMessageId()).isEqualTo(51L);
        assertThat(response.hidden()).isTrue();
        verify(dmHiddenBundleRepository).save(any(DmHiddenBundle.class));
    }

    @Test
    void synthesizeMessageAudioUsesSenderRepresentativeVoice() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        DmThread thread = reflectId(new DmThread(me, target), 41L);
        DmMessage aiMessage = reflectId(new DmMessage(thread, target, "음성으로 들려줄게", DmMessageKind.AI_PROXY), 61L);

        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(dmMessageRepository.findById(61L)).willReturn(Optional.of(aiMessage));
        given(voiceService.synthesizeDirectMessageText("음성으로 들려줄게", 2L)).willReturn(
                new VoiceSynthesisResult("voice-2", "audio/wav", "UklGRg==", null)
        );

        DmMessageAudioResponse response = dmService.synthesizeMessageAudio(1L, 61L);

        assertThat(response.messageId()).isEqualTo(61L);
        assertThat(response.voiceId()).isEqualTo("voice-2");
        assertThat(response.audioMimeType()).isEqualTo("audio/wav");
        assertThat(response.audioBase64()).isEqualTo("UklGRg==");
    }

    private UserAccount reflectId(UserAccount userAccount, Long id) {
        try {
            java.lang.reflect.Field idField = UserAccount.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(userAccount, id);
            return userAccount;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }

    private DmThread reflectId(DmThread thread, Long id) {
        try {
            java.lang.reflect.Field idField = DmThread.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(thread, id);
            return thread;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }

    private DmMessage reflectId(DmMessage message, Long id) {
        try {
            java.lang.reflect.Field idField = DmMessage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(message, id);
            return message;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }

    private PromptGenerationLog reflectId(PromptGenerationLog clone, Long id) {
        try {
            java.lang.reflect.Field idField = PromptGenerationLog.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(clone, id);
            return clone;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }

    private void reflectCreatedAt(DmThread thread, Instant createdAt) {
        try {
            java.lang.reflect.Field createdAtField = DmThread.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(thread, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign createdAt.", exception);
        }
    }

    private void reflectCreatedAt(DmMessage message, Instant createdAt) {
        try {
            java.lang.reflect.Field createdAtField = DmMessage.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(message, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign createdAt.", exception);
        }
    }

    private void reflectLastActivityAt(UserAccount userAccount, Instant lastActivityAt) {
        try {
            java.lang.reflect.Field lastActivityAtField = UserAccount.class.getDeclaredField("lastActivityAt");
            lastActivityAtField.setAccessible(true);
            lastActivityAtField.set(userAccount, lastActivityAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign lastActivityAt.", exception);
        }
    }
}
