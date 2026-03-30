package com.ssarvis.backend.post;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/api/posts")
    public PostSummaryResponse createPost(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return postService.createPost(user.userId(), request);
    }

    @GetMapping("/api/posts/feed")
    public List<PostSummaryResponse> feed(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return postService.listFeedPosts(user.userId());
    }

    @GetMapping("/api/posts/{postId}")
    public PostSummaryResponse getPost(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long postId
    ) {
        return postService.getPost(user.userId(), postId);
    }

    @GetMapping("/api/profiles/me/posts")
    public List<PostSummaryResponse> myPosts(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return postService.listProfilePosts(user.userId(), user.userId());
    }

    @GetMapping("/api/profiles/{profileUserId}/posts")
    public List<PostSummaryResponse> profilePosts(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long profileUserId
    ) {
        return postService.listProfilePosts(user.userId(), profileUserId);
    }

    @PatchMapping("/api/posts/{postId}")
    public PostSummaryResponse updatePost(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return postService.updatePost(user.userId(), postId, request);
    }

    @DeleteMapping("/api/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long postId
    ) {
        postService.deletePost(user.userId(), postId);
    }
}
