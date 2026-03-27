package com.ssarvis.backend.friend;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/requests")
    public FriendRequestResponse sendRequest(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @Valid @RequestBody CreateFriendRequestRequest request
    ) {
        return friendService.sendRequest(user.userId(), request);
    }

    @GetMapping("/requests/received")
    public List<FriendRequestResponse> listReceivedRequests(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return friendService.listReceivedRequests(user.userId());
    }

    @GetMapping("/requests/sent")
    public List<FriendRequestResponse> listSentRequests(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return friendService.listSentRequests(user.userId());
    }

    @PostMapping("/requests/{requestId}/accept")
    public FriendRequestResponse acceptRequest(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long requestId
    ) {
        return friendService.acceptRequest(user.userId(), requestId);
    }

    @PostMapping("/requests/{requestId}/reject")
    public FriendRequestResponse rejectRequest(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long requestId
    ) {
        return friendService.rejectRequest(user.userId(), requestId);
    }

    @PostMapping("/requests/{requestId}/cancel")
    public FriendRequestResponse cancelRequest(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long requestId
    ) {
        return friendService.cancelRequest(user.userId(), requestId);
    }

    @GetMapping
    public List<FriendSummaryResponse> listFriends(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return friendService.listFriends(user.userId());
    }

    @GetMapping("/users/search")
    public List<UserSearchResponse> searchUsers(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @RequestParam(value = "query", required = false) String query
    ) {
        return friendService.searchUsers(user.userId(), query);
    }
}
