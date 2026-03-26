package com.ssarvis.backend.debate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(DebateController.class)
@Import(GlobalExceptionHandler.class)
class DebateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DebateService debateService;

    @Test
    void listDebatesReturnsUserOwnedHistory() throws Exception {
        given(debateService.listDebates(1L)).willReturn(List.of(
                new DebateSessionSummaryResponse(9L, 1L, "하루", 2L, "미소", "원격근무가 더 효율적인가?", Instant.parse("2026-03-26T01:00:00Z"), 4)
        ));

        mockMvc.perform(get("/api/debates")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debateSessionId").value(9))
                .andExpect(jsonPath("$[0].cloneAAlias").value("하루"))
                .andExpect(jsonPath("$[0].turnCount").value(4));
    }

    @Test
    void getDebateReturnsTurnHistory() throws Exception {
        given(debateService.getDebate(1L, 9L)).willReturn(new DebateSessionDetailResponse(
                9L,
                1L,
                "하루",
                "차분한 클론",
                10L,
                2L,
                "미소",
                "창의적인 클론",
                11L,
                "원격근무가 더 효율적인가?",
                Instant.parse("2026-03-26T01:00:00Z"),
                List.of(
                        new DebateHistoryTurnResponse(1, "CLONE_A", 1L, "첫 발언", Instant.parse("2026-03-26T01:00:01Z"), null, null),
                        new DebateHistoryTurnResponse(2, "CLONE_B", 2L, "두 번째 발언", Instant.parse("2026-03-26T01:00:02Z"), "https://cdn.example/debate.mp3", "voice-b")
                )
        ));

        mockMvc.perform(get("/api/debates/9")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debateSessionId").value(9))
                .andExpect(jsonPath("$.turns[1].content").value("두 번째 발언"))
                .andExpect(jsonPath("$.turns[1].ttsAudioUrl").value("https://cdn.example/debate.mp3"));
    }

    @Test
    void startDebateReturnsFirstTurn() throws Exception {
        given(debateService.startDebate(eq(1L), any()))
                .willReturn(new DebateProgressResponse(
                        9L,
                        "원격근무가 더 효율적인가?",
                        new DebateTurnResponse(1, "CLONE_A", 1L, "저는 원격근무가 더 효율적이라고 봅니다.", "voice-a", "audio/wav", "UklG")
                ));

        mockMvc.perform(post("/api/debates")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cloneAId": 1,
                                  "cloneBId": 2,
                                  "cloneAVoiceId": 10,
                                  "cloneBVoiceId": 11,
                                  "topic": "원격근무가 더 효율적인가?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debateSessionId").value(9))
                .andExpect(jsonPath("$.turn.speaker").value("CLONE_A"))
                .andExpect(jsonPath("$.turn.ttsVoiceId").value("voice-a"));
    }

    @Test
    void createNextTurnReturnsSingleTurn() throws Exception {
        given(debateService.createNextTurn(1L, 9L))
                .willReturn(new DebateProgressResponse(
                        9L,
                        "원격근무가 더 효율적인가?",
                        new DebateTurnResponse(2, "CLONE_B", 2L, "저는 대면 협업의 효율이 더 높다고 봅니다.", "voice-b", "audio/wav", "UklH")
                ));

        mockMvc.perform(post("/api/debates/9/next")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debateSessionId").value(9))
                .andExpect(jsonPath("$.turn.speaker").value("CLONE_B"));
    }
}
