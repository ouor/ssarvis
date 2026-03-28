package com.ssarvis.backend.follow;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.AutoReplySettingsRequest;
import com.ssarvis.backend.auth.AutoReplySettingsResponse;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @GetMapping("/api/follows")
    public List<FollowUserSummaryResponse> listFollowing(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return followService.listFollowing(user.userId());
    }

    @PostMapping("/api/follows/{targetUserId}")
    public FollowUserSummaryResponse follow(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long targetUserId
    ) {
        return followService.follow(user.userId(), targetUserId);
    }

    @DeleteMapping("/api/follows/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long targetUserId
    ) {
        followService.unfollow(user.userId(), targetUserId);
    }

    @GetMapping("/api/follows/users/search")
    public List<FollowUserSummaryResponse> searchUsers(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @RequestParam(value = "query", required = false) String query
    ) {
        return followService.searchUsers(user.userId(), query);
    }

    @GetMapping("/api/profiles/me")
    public UserProfileResponse myProfile(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return followService.getMyProfile(user.userId());
    }

    @PatchMapping("/api/profiles/me/visibility")
    public UserProfileResponse updateVisibility(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @RequestBody VisibilityUpdateRequest request
    ) {
        return followService.updateMyVisibility(user.userId(), request);
    }

    @GetMapping("/api/profiles/{profileUserId}")
    public UserProfileResponse profile(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long profileUserId
    ) {
        return followService.getProfile(user.userId(), profileUserId);
    }

    @GetMapping("/api/profiles/me/auto-reply")
    public AutoReplySettingsResponse autoReplySettings(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return followService.getAutoReplySettings(user.userId());
    }

    @PatchMapping("/api/profiles/me/auto-reply")
    public AutoReplySettingsResponse updateAutoReplySettings(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @RequestBody AutoReplySettingsRequest request
    ) {
        return followService.updateAutoReplySettings(user.userId(), request);
    }
}
