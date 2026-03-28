package com.ssarvis.backend.follow;

import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.AutoReplyMode;
import com.ssarvis.backend.auth.AutoReplySettingsRequest;
import com.ssarvis.backend.auth.AutoReplySettingsResponse;
import com.ssarvis.backend.auth.UserAccount;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FollowService {

    private static final int USER_SEARCH_LIMIT = 20;

    private final FollowRepository followRepository;
    private final AuthService authService;

    public FollowService(FollowRepository followRepository, AuthService authService) {
        this.followRepository = followRepository;
        this.authService = authService;
    }

    @Transactional
    public FollowUserSummaryResponse follow(Long followerUserId, Long targetUserId) {
        if (followerUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot follow yourself.");
        }

        UserAccount follower = authService.getActiveUserAccount(followerUserId);
        UserAccount target = authService.getActiveUserAccount(targetUserId);

        if (target.getVisibility() == AccountVisibility.PRIVATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Private accounts cannot receive new follows.");
        }
        if (followRepository.existsByFollowerIdAndFolloweeId(followerUserId, targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already following this user.");
        }

        followRepository.save(new Follow(follower, target));
        return toSummary(target, true);
    }

    @Transactional
    public void unfollow(Long followerUserId, Long targetUserId) {
        authService.getActiveUserAccount(followerUserId);
        authService.getActiveUserAccount(targetUserId);
        Follow follow = followRepository.findByFollowerIdAndFolloweeId(followerUserId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follow relationship not found."));
        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public List<FollowUserSummaryResponse> listFollowing(Long followerUserId) {
        authService.getActiveUserAccount(followerUserId);
        return followRepository.findFollowingByFollowerId(followerUserId).stream()
                .map(follow -> toSummary(follow.getFollowee(), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowUserSummaryResponse> searchUsers(Long currentUserId, String query) {
        authService.getActiveUserAccount(currentUserId);
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : "";
        if (!StringUtils.hasText(normalizedQuery)) {
            return List.of();
        }

        return followRepository.searchDiscoverableUsers(currentUserId, normalizedQuery, PageRequest.of(0, USER_SEARCH_LIMIT)).stream()
                .map(user -> toSummary(user, followRepository.existsByFollowerIdAndFolloweeId(currentUserId, user.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long currentUserId) {
        UserAccount me = authService.getActiveUserAccount(currentUserId);
        return new UserProfileResponse(
                me.getId(),
                me.getUsername(),
                me.getDisplayName(),
                me.getVisibility(),
                true,
                false
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long currentUserId, Long profileUserId) {
        UserAccount viewer = authService.getActiveUserAccount(currentUserId);
        UserAccount profileOwner = authService.getActiveUserAccount(profileUserId);

        boolean me = viewer.getId().equals(profileOwner.getId());
        boolean following = followRepository.existsByFollowerIdAndFolloweeId(currentUserId, profileUserId);
        if (!me && profileOwner.getVisibility() == AccountVisibility.PRIVATE && !following) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This private profile is not available.");
        }

        return new UserProfileResponse(
                profileOwner.getId(),
                profileOwner.getUsername(),
                profileOwner.getDisplayName(),
                profileOwner.getVisibility(),
                me,
                following
        );
    }

    @Transactional
    public UserProfileResponse updateMyVisibility(Long currentUserId, VisibilityUpdateRequest request) {
        if (request == null || request.visibility() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "visibility is required.");
        }

        UserAccount me = authService.getActiveUserAccount(currentUserId);
        authService.updateVisibility(currentUserId, request.visibility());
        return new UserProfileResponse(
                me.getId(),
                me.getUsername(),
                me.getDisplayName(),
                request.visibility(),
                true,
                false
        );
    }

    @Transactional(readOnly = true)
    public AutoReplySettingsResponse getAutoReplySettings(Long currentUserId) {
        return authService.getAutoReplySettings(currentUserId);
    }

    @Transactional
    public AutoReplySettingsResponse updateAutoReplySettings(Long currentUserId, AutoReplySettingsRequest request) {
        if (request == null || request.mode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode is required.");
        }
        return authService.updateAutoReplySettings(currentUserId, normalizeMode(request.mode()));
    }

    private FollowUserSummaryResponse toSummary(UserAccount user, boolean following) {
        return new FollowUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getVisibility(),
                following
        );
    }

    private AutoReplyMode normalizeMode(AutoReplyMode mode) {
        return mode == null ? AutoReplyMode.OFF : mode;
    }
}
