package com.ssarvis.backend.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.friend.FriendRequestRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CloneAccessPolicyTest {

    @Mock
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    private CloneAccessPolicy cloneAccessPolicy;

    @BeforeEach
    void setUp() {
        cloneAccessPolicy = new CloneAccessPolicy(promptGenerationLogRepository, friendRequestRepository);
    }

    @Test
    void readableCloneAllowsOwner() {
        UserAccount owner = assignId(new UserAccount("haru", "hashed", "하루"), 1L);
        PromptGenerationLog clone = new PromptGenerationLog(owner, "gpt-5", "[]", "system", "하루 클론", "설명");
        given(promptGenerationLogRepository.findById(10L)).willReturn(Optional.of(clone));

        PromptGenerationLog readable = cloneAccessPolicy.getReadableClone(1L, 10L);

        assertThat(readable).isSameAs(clone);
    }

    @Test
    void readableCloneAllowsFriendEvenWhenPrivate() {
        UserAccount owner = assignId(new UserAccount("miso", "hashed", "미소"), 2L);
        PromptGenerationLog clone = new PromptGenerationLog(owner, "gpt-5", "[]", "system", "미소 클론", "설명");
        given(promptGenerationLogRepository.findById(11L)).willReturn(Optional.of(clone));
        given(friendRequestRepository.existsAcceptedBetweenUsers(1L, 2L)).willReturn(true);

        PromptGenerationLog readable = cloneAccessPolicy.getReadableClone(1L, 11L);

        assertThat(readable).isSameAs(clone);
    }

    @Test
    void readableCloneRejectsNonFriendPrivateClone() {
        UserAccount owner = assignId(new UserAccount("miso", "hashed", "미소"), 2L);
        PromptGenerationLog clone = new PromptGenerationLog(owner, "gpt-5", "[]", "system", "미소 클론", "설명");
        given(promptGenerationLogRepository.findById(12L)).willReturn(Optional.of(clone));
        given(friendRequestRepository.existsAcceptedBetweenUsers(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> cloneAccessPolicy.getReadableClone(1L, 12L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Prompt generation log not found.");
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
