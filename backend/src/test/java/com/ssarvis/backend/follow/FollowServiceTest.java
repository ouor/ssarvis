package com.ssarvis.backend.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
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
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private AuthService authService;

    private FollowService followService;

    @BeforeEach
    void setUp() {
        followService = new FollowService(followRepository, authService);
    }

    @Test
    void followCreatesRelationshipForPublicAccount() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(authService.getActiveUserAccount(2L)).willReturn(target);
        given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(false);
        given(followRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        FollowUserSummaryResponse response = followService.follow(1L, 2L);

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.following()).isTrue();
        assertThat(response.visibility()).isEqualTo(AccountVisibility.PUBLIC);
    }

    @Test
    void followRejectsPrivateAccount() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        target.updateVisibility(AccountVisibility.PRIVATE);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(authService.getActiveUserAccount(2L)).willReturn(target);

        assertThatThrownBy(() -> followService.follow(1L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("Private accounts cannot receive new follows.");
                });
    }

    @Test
    void searchUsersReturnsDiscoverableUsersWithFollowingState() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(followRepository.searchDiscoverableUsers(any(), any(), any())).willReturn(List.of(target));
        given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(true);

        List<FollowUserSummaryResponse> results = followService.searchUsers(1L, "미");

        assertThat(results).containsExactly(new FollowUserSummaryResponse(2L, "miso", "미소", AccountVisibility.PUBLIC, true));
    }

    @Test
    void getProfileRejectsUnfollowedPrivateUser() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        target.updateVisibility(AccountVisibility.PRIVATE);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(authService.getActiveUserAccount(2L)).willReturn(target);
        given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> followService.getProfile(1L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("This private profile is not available.");
                });
    }

    @Test
    void updateMyVisibilityReturnsUpdatedProfile() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        given(authService.getActiveUserAccount(1L)).willReturn(me);

        UserProfileResponse response = followService.updateMyVisibility(1L, new VisibilityUpdateRequest(AccountVisibility.PRIVATE));

        assertThat(response.visibility()).isEqualTo(AccountVisibility.PRIVATE);
        assertThat(response.me()).isTrue();
    }

    @Test
    void unfollowDeletesExistingRelationship() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount target = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        Follow follow = new Follow(me, target);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(authService.getActiveUserAccount(2L)).willReturn(target);
        given(followRepository.findByFollowerIdAndFolloweeId(1L, 2L)).willReturn(Optional.of(follow));

        followService.unfollow(1L, 2L);

        verify(followRepository).delete(follow);
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
}
