package com.ssarvis.backend.post;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.api.GlobalExceptionHandler;
import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostController.class)
@Import(GlobalExceptionHandler.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @Test
    void createPostReturnsCreatedSummary() throws Exception {
        given(postService.createPost(any(), any())).willReturn(postResponse(11L, 1L, "haru", "하루", "새 게시물"));

        mockMvc.perform(post("/api/posts")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "새 게시물"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(11))
                .andExpect(jsonPath("$.content").value("새 게시물"));
    }

    @Test
    void feedReturnsVisiblePosts() throws Exception {
        given(postService.listFeedPosts(1L)).willReturn(List.of(postResponse(21L, 2L, "miso", "미소", "피드 게시물")));

        mockMvc.perform(get("/api/posts/feed")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerDisplayName").value("미소"));
    }

    @Test
    void myPostsReturnsProfilePosts() throws Exception {
        given(postService.listProfilePosts(1L, 1L)).willReturn(List.of(postResponse(31L, 1L, "haru", "하루", "내 게시물")));

        mockMvc.perform(get("/api/profiles/me/posts")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerUserId").value(1));
    }

    @Test
    void profilePostsReturnsVisiblePosts() throws Exception {
        given(postService.listProfilePosts(1L, 2L)).willReturn(List.of(postResponse(41L, 2L, "miso", "미소", "프로필 게시물")));

        mockMvc.perform(get("/api/profiles/2/posts")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("프로필 게시물"));
    }

    private AuthenticatedUser authUser() {
        return new AuthenticatedUser(1L, "haru", "하루", AccountVisibility.PUBLIC);
    }

    private PostSummaryResponse postResponse(Long postId, Long ownerUserId, String username, String displayName, String content) {
        return new PostSummaryResponse(
                postId,
                ownerUserId,
                username,
                displayName,
                AccountVisibility.PUBLIC,
                content,
                Instant.parse("2026-03-28T00:00:00Z")
        );
    }
}
