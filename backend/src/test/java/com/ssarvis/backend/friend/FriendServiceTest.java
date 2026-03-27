package com.ssarvis.backend.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private AuthService authService;

    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendService = new FriendService(friendRequestRepository, authService);
    }

    @Test
    void sendRequestCreatesPendingFriendRequest() {
        UserAccount requester = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount receiver = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        given(authService.getActiveUserAccount(1L)).willReturn(requester);
        given(authService.getActiveUserAccount(2L)).willReturn(receiver);
        given(friendRequestRepository.existsAcceptedBetweenUsers(1L, 2L)).willReturn(false);
        given(friendRequestRepository.findPendingRequest(2L, 1L)).willReturn(Optional.empty());
        given(friendRequestRepository.findPendingRequest(1L, 2L)).willReturn(Optional.empty());
        given(friendRequestRepository.save(any())).willAnswer(invocation -> {
            FriendRequest request = invocation.getArgument(0);
            reflectId(request, 11L);
            reflectCreatedAt(request, Instant.now());
            return request;
        });

        FriendRequestResponse response = friendService.sendRequest(1L, new CreateFriendRequestRequest(2L));

        assertThat(response.friendRequestId()).isEqualTo(11L);
        assertThat(response.status()).isEqualTo(FriendRequestStatus.PENDING);
        assertThat(response.requester().userId()).isEqualTo(1L);
        assertThat(response.receiver().userId()).isEqualTo(2L);
    }

    @Test
    void sendRequestRejectsSelfRequest() {
        assertThatThrownBy(() -> friendService.sendRequest(1L, new CreateFriendRequestRequest(1L)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("You cannot send a friend request to yourself.");
                });
    }

    @Test
    void sendRequestGuidesUserToAcceptInversePendingRequest() {
        UserAccount requester = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount receiver = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        given(authService.getActiveUserAccount(1L)).willReturn(requester);
        given(authService.getActiveUserAccount(2L)).willReturn(receiver);
        given(friendRequestRepository.existsAcceptedBetweenUsers(1L, 2L)).willReturn(false);
        given(friendRequestRepository.findPendingRequest(2L, 1L))
                .willReturn(Optional.of(new FriendRequest(receiver, requester)));

        assertThatThrownBy(() -> friendService.sendRequest(1L, new CreateFriendRequestRequest(2L)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("This user already sent you a friend request. Accept it instead.");
                });
    }

    @Test
    void acceptRequestMarksRequestAccepted() {
        UserAccount requester = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount receiver = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        FriendRequest friendRequest = reflectId(new FriendRequest(requester, receiver), 21L);
        reflectCreatedAt(friendRequest, Instant.now().minusSeconds(60));
        given(authService.getActiveUserAccount(2L)).willReturn(receiver);
        given(friendRequestRepository.findPendingByIdAndReceiverId(21L, 2L)).willReturn(Optional.of(friendRequest));
        given(friendRequestRepository.save(friendRequest)).willReturn(friendRequest);

        FriendRequestResponse response = friendService.acceptRequest(2L, 21L);

        assertThat(response.status()).isEqualTo(FriendRequestStatus.ACCEPTED);
        assertThat(response.respondedAt()).isNotNull();
    }

    @Test
    void cancelRequestMarksRequestCanceled() {
        UserAccount requester = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount receiver = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        FriendRequest friendRequest = reflectId(new FriendRequest(requester, receiver), 22L);
        reflectCreatedAt(friendRequest, Instant.now().minusSeconds(60));
        given(authService.getActiveUserAccount(1L)).willReturn(requester);
        given(friendRequestRepository.findPendingByIdAndRequesterId(22L, 1L)).willReturn(Optional.of(friendRequest));
        given(friendRequestRepository.save(friendRequest)).willReturn(friendRequest);

        FriendRequestResponse response = friendService.cancelRequest(1L, 22L);

        assertThat(response.status()).isEqualTo(FriendRequestStatus.CANCELED);
        assertThat(response.respondedAt()).isNotNull();
    }

    @Test
    void listFriendsReturnsAcceptedCounterpart() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount friend = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        FriendRequest accepted = reflectId(new FriendRequest(me, friend), 31L);
        accepted.accept();
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(friendRequestRepository.findAcceptedByUserId(1L)).willReturn(List.of(accepted));

        List<FriendSummaryResponse> friends = friendService.listFriends(1L);

        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).user().userId()).isEqualTo(2L);
        assertThat(friends.get(0).user().displayName()).isEqualTo("미소");
    }

    @Test
    void searchUsersReturnsEmptyWhenQueryIsBlank() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        given(authService.getActiveUserAccount(1L)).willReturn(me);

        List<UserSearchResponse> results = friendService.searchUsers(1L, "   ");

        assertThat(results).isEmpty();
    }

    @Test
    void searchUsersReturnsMatchedActiveUsers() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount match = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(authService.searchActiveUsers(any(), any(), any(Pageable.class))).willReturn(List.of(match));

        List<UserSearchResponse> results = friendService.searchUsers(1L, "미");

        assertThat(results).containsExactly(new UserSearchResponse(2L, "miso", "미소"));
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

    private FriendRequest reflectId(FriendRequest friendRequest, Long id) {
        try {
            java.lang.reflect.Field idField = FriendRequest.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(friendRequest, id);
            return friendRequest;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }

    private void reflectCreatedAt(FriendRequest friendRequest, Instant createdAt) {
        try {
            java.lang.reflect.Field createdAtField = FriendRequest.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(friendRequest, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign createdAt.", exception);
        }
    }
}
