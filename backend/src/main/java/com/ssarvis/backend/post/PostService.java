package com.ssarvis.backend.post;

import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.follow.FollowRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final AuthService authService;
    private final FollowRepository followRepository;

    public PostService(PostRepository postRepository, AuthService authService, FollowRepository followRepository) {
        this.postRepository = postRepository;
        this.authService = authService;
        this.followRepository = followRepository;
    }

    @Transactional
    public PostSummaryResponse createPost(Long userId, CreatePostRequest request) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank.");
        }

        UserAccount user = authService.getActiveUserAccount(userId);
        Post post = postRepository.save(new Post(user, request.content().trim()));
        return toSummary(post);
    }

    @Transactional(readOnly = true)
    public List<PostSummaryResponse> listFeedPosts(Long currentUserId) {
        authService.getActiveUserAccount(currentUserId);
        return postRepository.findVisibleFeedPosts(currentUserId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostSummaryResponse> listProfilePosts(Long currentUserId, Long profileUserId) {
        UserAccount viewer = authService.getActiveUserAccount(currentUserId);
        UserAccount owner = authService.getActiveUserAccount(profileUserId);

        boolean me = viewer.getId().equals(owner.getId());
        boolean following = followRepository.existsByFollowerIdAndFolloweeId(currentUserId, profileUserId);
        if (!me && owner.getVisibility() == AccountVisibility.PRIVATE && !following) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This private profile is not available.");
        }

        return postRepository.findAllByOwnerIdOrderByCreatedAtDesc(profileUserId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostSummaryResponse getPost(Long currentUserId, Long postId) {
        UserAccount viewer = authService.getActiveUserAccount(currentUserId);
        Post post = getVisiblePost(viewer.getId(), postId);
        return toSummary(post);
    }

    @Transactional
    public PostSummaryResponse updatePost(Long userId, Long postId, UpdatePostRequest request) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank.");
        }

        authService.getActiveUserAccount(userId);
        Post post = getOwnedPost(userId, postId);
        post.updateContent(request.content().trim());
        return toSummary(post);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        authService.getActiveUserAccount(userId);
        Post post = getOwnedPost(userId, postId);
        postRepository.delete(post);
    }

    private Post getVisiblePost(Long currentUserId, Long postId) {
        Post post = postRepository.findByIdWithOwner(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found."));

        Long ownerUserId = post.getUser().getId();
        boolean me = ownerUserId.equals(currentUserId);
        boolean following = followRepository.existsByFollowerIdAndFolloweeId(currentUserId, ownerUserId);

        if (!me && post.getUser().getVisibility() == AccountVisibility.PRIVATE && !following) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This post is not available.");
        }

        return post;
    }

    private Post getOwnedPost(Long userId, Long postId) {
        Post post = postRepository.findByIdWithOwner(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found."));

        if (!post.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own posts.");
        }

        return post;
    }

    private PostSummaryResponse toSummary(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getDisplayName(),
                post.getUser().getVisibility(),
                post.getContent(),
                post.getCreatedAt()
        );
    }
}
