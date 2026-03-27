package com.ssarvis.backend.friend;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FriendRelationshipService {

    private final FriendRequestRepository friendRequestRepository;

    public FriendRelationshipService(FriendRequestRepository friendRequestRepository) {
        this.friendRequestRepository = friendRequestRepository;
    }

    public List<Long> findAcceptedFriendIds(Long userId) {
        return friendRequestRepository.findAcceptedFriendIds(userId);
    }

    public boolean areFriends(Long firstUserId, Long secondUserId) {
        return friendRequestRepository.existsAcceptedBetweenUsers(firstUserId, secondUserId);
    }
}
