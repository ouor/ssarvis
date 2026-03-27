package com.ssarvis.backend.friend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.api.GlobalExceptionHandler;
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

@WebMvcTest(FriendController.class)
@Import(GlobalExceptionHandler.class)
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendService friendService;

    @Test
    void sendRequestReturnsCreatedFriendRequest() throws Exception {
        given(friendService.sendRequest(any(), any()))
                .willReturn(requestResponse(FriendRequestStatus.PENDING));

        mockMvc.perform(post("/api/friends/requests")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, new AuthenticatedUser(1L, "haru", "하루"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverUserId": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendRequestId").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requester.userId").value(1))
                .andExpect(jsonPath("$.receiver.userId").value(2));
    }

    @Test
    void listReceivedRequestsReturnsPendingRequests() throws Exception {
        given(friendService.listReceivedRequests(1L)).willReturn(List.of(requestResponse(FriendRequestStatus.PENDING)));

        mockMvc.perform(get("/api/friends/requests/received")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, new AuthenticatedUser(1L, "haru", "하루")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].friendRequestId").value(10));
    }

    @Test
    void listSentRequestsReturnsPendingRequests() throws Exception {
        given(friendService.listSentRequests(1L)).willReturn(List.of(requestResponse(FriendRequestStatus.PENDING)));

        mockMvc.perform(get("/api/friends/requests/sent")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, new AuthenticatedUser(1L, "haru", "하루")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void acceptRequestReturnsUpdatedStatus() throws Exception {
        given(friendService.acceptRequest(1L, 10L)).willReturn(requestResponse(FriendRequestStatus.ACCEPTED));

        mockMvc.perform(post("/api/friends/requests/10/accept")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, new AuthenticatedUser(1L, "haru", "하루")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void rejectRequestReturnsUpdatedStatus() throws Exception {
        given(friendService.rejectRequest(1L, 10L)).willReturn(requestResponse(FriendRequestStatus.REJECTED));

        mockMvc.perform(post("/api/friends/requests/10/reject")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, new AuthenticatedUser(1L, "haru", "하루")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void cancelRequestReturnsUpdatedStatus() throws Exception {
        given(friendService.cancelRequest(1L, 10L)).willReturn(requestResponse(FriendRequestStatus.CANCELED));

        mockMvc.perform(post("/api/friends/requests/10/cancel")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, new AuthenticatedUser(1L, "haru", "하루")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void listFriendsReturnsFriendSummaries() throws Exception {
        given(friendService.listFriends(1L)).willReturn(List.of(
                new FriendSummaryResponse(new FriendUserSummaryResponse(2L, "miso", "미소"), Instant.parse("2026-03-27T00:00:00Z"))
        ));

        mockMvc.perform(get("/api/friends")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, new AuthenticatedUser(1L, "haru", "하루")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user.userId").value(2))
                .andExpect(jsonPath("$[0].user.displayName").value("미소"));
    }

    @Test
    void searchUsersReturnsMatchedUsers() throws Exception {
        given(friendService.searchUsers(1L, "미")).willReturn(List.of(
                new UserSearchResponse(2L, "miso", "미소")
        ));

        mockMvc.perform(get("/api/friends/users/search")
                        .param("query", "미")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, new AuthenticatedUser(1L, "haru", "하루")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].username").value("miso"));
    }

    private FriendRequestResponse requestResponse(FriendRequestStatus status) {
        return new FriendRequestResponse(
                10L,
                status,
                Instant.parse("2026-03-27T00:00:00Z"),
                status == FriendRequestStatus.PENDING ? null : Instant.parse("2026-03-27T01:00:00Z"),
                new FriendUserSummaryResponse(1L, "haru", "하루"),
                new FriendUserSummaryResponse(2L, "miso", "미소")
        );
    }
}
