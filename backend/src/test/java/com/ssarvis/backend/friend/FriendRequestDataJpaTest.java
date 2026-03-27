package com.ssarvis.backend.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.auth.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class FriendRequestDataJpaTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Test
    void persistsFriendRequestWithPendingStatusByDefault() {
        UserAccount requester = userAccountRepository.save(new UserAccount("friend-a", "hashed", "친구A"));
        UserAccount receiver = userAccountRepository.save(new UserAccount("friend-b", "hashed", "친구B"));

        FriendRequest friendRequest = friendRequestRepository.saveAndFlush(new FriendRequest(requester, receiver));

        assertThat(friendRequest.getId()).isNotNull();
        assertThat(friendRequest.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
        assertThat(friendRequest.getCreatedAt()).isNotNull();
        assertThat(friendRequest.getRespondedAt()).isNull();
        assertThat(friendRequestRepository.existsPendingBetweenUsers(requester.getId(), receiver.getId())).isTrue();
        assertThat(friendRequestRepository.findPendingRequest(requester.getId(), receiver.getId())).contains(friendRequest);
    }

    @Test
    void rejectsSelfFriendRequest() {
        UserAccount user = userAccountRepository.save(new UserAccount("friend-self", "hashed", "나"));

        assertThatThrownBy(() -> new FriendRequest(user, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot create a friend request to the same user.");
    }

    @Test
    void disallowsDuplicatePendingRequestsForSamePairRegardlessOfDirection() {
        UserAccount requester = userAccountRepository.save(new UserAccount("friend-c", "hashed", "친구C"));
        UserAccount receiver = userAccountRepository.save(new UserAccount("friend-d", "hashed", "친구D"));

        friendRequestRepository.saveAndFlush(new FriendRequest(requester, receiver));

        assertThat(friendRequestRepository.existsPendingBetweenUsers(requester.getId(), receiver.getId())).isTrue();
        assertThat(friendRequestRepository.existsPendingBetweenUsers(receiver.getId(), requester.getId())).isTrue();

        assertThatThrownBy(() -> friendRequestRepository.saveAndFlush(new FriendRequest(receiver, requester)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsNewPendingRequestAfterPreviousRequestIsRejected() {
        UserAccount requester = userAccountRepository.save(new UserAccount("friend-e", "hashed", "친구E"));
        UserAccount receiver = userAccountRepository.save(new UserAccount("friend-f", "hashed", "친구F"));

        FriendRequest firstRequest = friendRequestRepository.saveAndFlush(new FriendRequest(requester, receiver));
        firstRequest.reject();
        friendRequestRepository.saveAndFlush(firstRequest);

        FriendRequest nextRequest = friendRequestRepository.saveAndFlush(new FriendRequest(requester, receiver));

        assertThat(firstRequest.getStatus()).isEqualTo(FriendRequestStatus.REJECTED);
        assertThat(firstRequest.getRespondedAt()).isNotNull();
        assertThat(nextRequest.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
        assertThat(nextRequest.getId()).isNotEqualTo(firstRequest.getId());
    }
}
