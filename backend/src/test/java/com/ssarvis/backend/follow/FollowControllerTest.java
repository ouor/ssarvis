package com.ssarvis.backend.follow;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.api.GlobalExceptionHandler;
import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.AutoReplyMode;
import com.ssarvis.backend.auth.AutoReplySettingsResponse;
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

@WebMvcTest(FollowController.class)
@Import(GlobalExceptionHandler.class)
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FollowService followService;

    @Test
    void listFollowingReturnsSummaries() throws Exception {
        given(followService.listFollowing(1L)).willReturn(List.of(
                new FollowUserSummaryResponse(2L, "miso", "미소", AccountVisibility.PUBLIC, true)
        ));

        mockMvc.perform(get("/api/follows")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].following").value(true));
    }

    @Test
    void followReturnsSummary() throws Exception {
        given(followService.follow(1L, 2L)).willReturn(new FollowUserSummaryResponse(2L, "miso", "미소", AccountVisibility.PUBLIC, true));

        mockMvc.perform(post("/api/follows/2")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    void unfollowReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/follows/2")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isNoContent());

        verify(followService).unfollow(1L, 2L);
    }

    @Test
    void searchUsersReturnsDiscoverableUsers() throws Exception {
        given(followService.searchUsers(1L, "미")).willReturn(List.of(
                new FollowUserSummaryResponse(2L, "miso", "미소", AccountVisibility.PUBLIC, false)
        ));

        mockMvc.perform(get("/api/follows/users/search")
                        .param("query", "미")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("miso"));
    }

    @Test
    void myProfileReturnsVisibility() throws Exception {
        given(followService.getMyProfile(1L)).willReturn(new UserProfileResponse(1L, "haru", "하루", AccountVisibility.PRIVATE, true, false));

        mockMvc.perform(get("/api/profiles/me")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.me").value(true));
    }

    @Test
    void updateVisibilityReturnsUpdatedProfile() throws Exception {
        given(followService.updateMyVisibility(1L, new VisibilityUpdateRequest(AccountVisibility.PRIVATE)))
                .willReturn(new UserProfileResponse(1L, "haru", "하루", AccountVisibility.PRIVATE, true, false));

        mockMvc.perform(patch("/api/profiles/me/visibility")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    @Test
    void profileReturnsAccessibleSummary() throws Exception {
        given(followService.getProfile(1L, 2L)).willReturn(new UserProfileResponse(2L, "miso", "미소", AccountVisibility.PUBLIC, false, true));

        mockMvc.perform(get("/api/profiles/2")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.following").value(true));
    }

    @Test
    void autoReplySettingsReturnsCurrentMode() throws Exception {
        given(followService.getAutoReplySettings(1L)).willReturn(new AutoReplySettingsResponse(AutoReplyMode.AWAY, Instant.parse("2026-03-28T00:00:00Z")));

        mockMvc.perform(get("/api/profiles/me/auto-reply")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("AWAY"));
    }

    @Test
    void updateAutoReplySettingsReturnsUpdatedMode() throws Exception {
        given(followService.updateAutoReplySettings(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .willReturn(new AutoReplySettingsResponse(AutoReplyMode.ALWAYS, Instant.parse("2026-03-28T00:00:00Z")));

        mockMvc.perform(patch("/api/profiles/me/auto-reply")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "ALWAYS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ALWAYS"));
    }

    private AuthenticatedUser authUser() {
        return new AuthenticatedUser(1L, "haru", "하루", AccountVisibility.PUBLIC);
    }
}
