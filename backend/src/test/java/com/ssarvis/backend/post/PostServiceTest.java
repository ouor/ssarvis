package com.ssarvis.backend.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.follow.FollowRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private AuthService authService;

    @Mock
    private FollowRepository followRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, authService, followRepository);
    }

    @Test
    void createPostPersistsTrimmedContent() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(postRepository.save(any())).willAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            reflectId(post, 7L);
            reflectCreatedAt(post, Instant.parse("2026-03-28T00:00:00Z"));
            return post;
        });

        PostSummaryResponse response = postService.createPost(1L, new CreatePostRequest("  첫 번째 게시물  "));

        assertThat(response.postId()).isEqualTo(7L);
        assertThat(response.content()).isEqualTo("첫 번째 게시물");
        assertThat(response.ownerUserId()).isEqualTo(1L);
    }

    @Test
    void listProfilePostsRejectsUnfollowedPrivateAccount() {
        UserAccount me = reflectId(new UserAccount("haru", "hashed", "하루"), 1L);
        UserAccount owner = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        owner.updateVisibility(AccountVisibility.PRIVATE);
        given(authService.getActiveUserAccount(1L)).willReturn(me);
        given(authService.getActiveUserAccount(2L)).willReturn(owner);
        given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> postService.listProfilePosts(1L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("This private profile is not available.");
                });
    }

    @Test
    void listFeedPostsReturnsVisiblePosts() {
        UserAccount owner = reflectId(new UserAccount("miso", "hashed", "미소"), 2L);
        Post post = new Post(owner, "공개 게시물");
        reflectId(post, 9L);
        reflectCreatedAt(post, Instant.parse("2026-03-28T01:00:00Z"));
        given(authService.getActiveUserAccount(1L)).willReturn(reflectId(new UserAccount("haru", "hashed", "하루"), 1L));
        given(postRepository.findVisibleFeedPosts(1L)).willReturn(List.of(post));

        List<PostSummaryResponse> results = postService.listFeedPosts(1L);

        assertThat(results).containsExactly(
                new PostSummaryResponse(9L, 2L, "miso", "미소", AccountVisibility.PUBLIC, "공개 게시물", Instant.parse("2026-03-28T01:00:00Z"))
        );
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

    private Post reflectId(Post post, Long id) {
        try {
            java.lang.reflect.Field idField = Post.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(post, id);
            return post;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }

    private void reflectCreatedAt(Post post, Instant createdAt) {
        try {
            java.lang.reflect.Field createdAtField = Post.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(post, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign createdAt.", exception);
        }
    }
}
