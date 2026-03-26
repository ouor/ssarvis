package com.ssarvis.backend.debate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.api.GlobalExceptionHandler;
import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
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
