package com.ssarvis.backend.friend;

import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FriendService {

    private static final int USER_SEARCH_LIMIT = 20;

    private final FriendRequestRepository friendRequestRepository;
    private final AuthService authService;

    public FriendService(FriendRequestRepository friendRequestRepository, AuthService authService) {
        this.friendRequestRepository = friendRequestRepository;
        this.authService = authService;
    }

    @Transactional
    public FriendRequestResponse sendRequest(Long requesterUserId, CreateFriendRequestRequest request) {
        if (request == null || request.receiverUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receiverUserId is required.");
        }
        if (requesterUserId.equals(request.receiverUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a friend request to yourself.");
        }

        UserAccount requester = authService.getActiveUserAccount(requesterUserId);
        UserAccount receiver = authService.getActiveUserAccount(request.receiverUserId());

        if (friendRequestRepository.existsAcceptedBetweenUsers(requesterUserId, receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already friends.");
        }
        if (friendRequestRepository.findPendingRequest(receiver.getId(), requesterUserId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This user already sent you a friend request. Accept it instead.");
        }
        if (friendRequestRepository.findPendingRequest(requesterUserId, receiver.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request is already pending.");
        }

        FriendRequest friendRequest = friendRequestRepository.save(new FriendRequest(requester, receiver));
        return toRequestResponse(friendRequest);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> listReceivedRequests(Long userId) {
        authService.getActiveUserAccount(userId);
        return friendRequestRepository.findPendingReceivedByReceiverId(userId).stream()
                .map(this::toRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> listSentRequests(Long userId) {
        authService.getActiveUserAccount(userId);
        return friendRequestRepository.findPendingSentByRequesterId(userId).stream()
                .map(this::toRequestResponse)
                .toList();
    }

    @Transactional
    public FriendRequestResponse acceptRequest(Long receiverUserId, Long requestId) {
        authService.getActiveUserAccount(receiverUserId);
        FriendRequest friendRequest = friendRequestRepository.findPendingByIdAndReceiverId(requestId, receiverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found."));
        friendRequest.accept();
        return toRequestResponse(friendRequestRepository.save(friendRequest));
    }

    @Transactional
    public FriendRequestResponse rejectRequest(Long receiverUserId, Long requestId) {
        authService.getActiveUserAccount(receiverUserId);
        FriendRequest friendRequest = friendRequestRepository.findPendingByIdAndReceiverId(requestId, receiverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found."));
        friendRequest.reject();
        return toRequestResponse(friendRequestRepository.save(friendRequest));
    }

    @Transactional
    public FriendRequestResponse cancelRequest(Long requesterUserId, Long requestId) {
        authService.getActiveUserAccount(requesterUserId);
        FriendRequest friendRequest = friendRequestRepository.findPendingByIdAndRequesterId(requestId, requesterUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found."));
        friendRequest.cancel();
        return toRequestResponse(friendRequestRepository.save(friendRequest));
    }

    @Transactional(readOnly = true)
    public List<FriendSummaryResponse> listFriends(Long userId) {
        authService.getActiveUserAccount(userId);
        return friendRequestRepository.findAcceptedByUserId(userId).stream()
                .map(request -> toFriendSummary(userId, request))
                .toList();
    }

    @Transactional
    public FriendRequestResponse unfriend(Long userId, Long friendUserId) {
        authService.getActiveUserAccount(userId);
        authService.getActiveUserAccount(friendUserId);
        FriendRequest friendRequest = friendRequestRepository.findAcceptedBetweenUsers(userId, friendUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found."));
        friendRequest.unfriend();
        return toRequestResponse(friendRequestRepository.save(friendRequest));
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(Long userId, String query) {
        authService.getActiveUserAccount(userId);
        String normalizedQuery = normalize(query);
        if (!StringUtils.hasText(normalizedQuery)) {
            return List.of();
        }
        return authService.searchActiveUsers(userId, normalizedQuery, PageRequest.of(0, USER_SEARCH_LIMIT)).stream()
                .map(user -> new UserSearchResponse(user.getId(), user.getUsername(), user.getDisplayName()))
                .toList();
    }

    private FriendRequestResponse toRequestResponse(FriendRequest request) {
        return new FriendRequestResponse(
                request.getId(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getRespondedAt(),
                toUserSummary(request.getRequester()),
                toUserSummary(request.getReceiver())
        );
    }

    private FriendSummaryResponse toFriendSummary(Long currentUserId, FriendRequest request) {
        UserAccount friend = request.getRequester().getId().equals(currentUserId)
                ? request.getReceiver()
                : request.getRequester();
        return new FriendSummaryResponse(
                toUserSummary(friend),
                request.getRespondedAt() != null ? request.getRespondedAt() : request.getCreatedAt()
        );
    }

    private FriendUserSummaryResponse toUserSummary(UserAccount user) {
        return new FriendUserSummaryResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
