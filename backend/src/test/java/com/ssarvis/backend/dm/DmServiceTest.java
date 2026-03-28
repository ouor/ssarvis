package com.ssarvis.backend.dm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.follow.FollowRepository;
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

    private DmService dmService;

    @BeforeEach
    void setUp() {
        dmService = new DmService(dmThreadRepository, dmMessageRepository, authService, followRepository);
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
        assertThat(response.content()).isEqualTo("안녕!");
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
}
