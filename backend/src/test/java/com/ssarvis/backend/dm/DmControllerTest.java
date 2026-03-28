package com.ssarvis.backend.dm;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DmController.class)
@Import(GlobalExceptionHandler.class)
class DmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DmService dmService;

    @Test
    void startThreadReturnsDetail() throws Exception {
        given(dmService.startThread(1L, new DmStartRequest(2L))).willReturn(detailResponse());

        mockMvc.perform(post("/api/dms/threads")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserId": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threadId").value(10))
                .andExpect(jsonPath("$.otherParticipant.userId").value(2));
    }

    @Test
    void listThreadsReturnsSummaries() throws Exception {
        given(dmService.listThreads(1L)).willReturn(List.of(summaryResponse()));

        mockMvc.perform(get("/api/dms/threads")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latestMessagePreview").value("최근 메시지"));
    }

    @Test
    void getThreadReturnsDetail() throws Exception {
        given(dmService.getThread(1L, 10L)).willReturn(detailResponse());

        mockMvc.perform(get("/api/dms/threads/10")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].content").value("안녕!"));
    }

    @Test
    void sendMessageReturnsCreatedMessage() throws Exception {
        given(dmService.sendMessage(1L, 10L, new DmSendMessageRequest("안녕!"))).willReturn(
                new DmMessageResponse(30L, 1L, "하루", false, null, "TEXT", null, null, "안녕!", Instant.parse("2026-03-28T00:01:00Z"))
        );

        mockMvc.perform(post("/api/dms/threads/10/messages")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "안녕!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(30))
                .andExpect(jsonPath("$.senderUserId").value(1))
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.format").value("TEXT"));
    }

    @Test
    void sendVoiceMessageReturnsCreatedVoiceMessage() throws Exception {
        MockMultipartFile audio = new MockMultipartFile("audio", "voice.wav", "audio/wav", new byte[] {1, 2, 3});
        given(dmService.sendVoiceMessage(1L, 10L, audio)).willReturn(
                new DmMessageResponse(31L, 1L, "하루", false, null, "VOICE", "audio/wav", "AQID", "음성 메시지", Instant.parse("2026-03-28T00:01:00Z"))
        );

        mockMvc.perform(multipart("/api/dms/threads/10/voice-messages")
                        .file(audio)
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(31))
                .andExpect(jsonPath("$.format").value("VOICE"))
                .andExpect(jsonPath("$.audioMimeType").value("audio/wav"))
                .andExpect(jsonPath("$.audioBase64").value("AQID"));
    }

    @Test
    void hideBundleReturnsVisibilityState() throws Exception {
        given(dmService.hideBundle(1L, 10L, 30L)).willReturn(new DmBundleVisibilityResponse(30L, true));

        mockMvc.perform(post("/api/dms/threads/10/bundles/30/hide")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleRootMessageId").value(30))
                .andExpect(jsonPath("$.hidden").value(true));
    }

    @Test
    void showBundleReturnsVisibilityState() throws Exception {
        given(dmService.showBundle(1L, 10L, 30L)).willReturn(new DmBundleVisibilityResponse(30L, false));

        mockMvc.perform(delete("/api/dms/threads/10/bundles/30/hide")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidden").value(false));
    }

    @Test
    void synthesizeMessageAudioReturnsPlayablePayload() throws Exception {
        given(dmService.synthesizeMessageAudio(1L, 30L)).willReturn(new DmMessageAudioResponse(30L, "voice-demo", "audio/wav", "UklGRg=="));

        mockMvc.perform(post("/api/dms/messages/30/tts")
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE, authUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(30))
                .andExpect(jsonPath("$.voiceId").value("voice-demo"))
                .andExpect(jsonPath("$.audioMimeType").value("audio/wav"))
                .andExpect(jsonPath("$.audioBase64").value("UklGRg=="));
    }

    private AuthenticatedUser authUser() {
        return new AuthenticatedUser(1L, "haru", "하루", AccountVisibility.PUBLIC);
    }

    private DmThreadSummaryResponse summaryResponse() {
        return new DmThreadSummaryResponse(
                10L,
                new DmParticipantResponse(2L, "miso", "미소", AccountVisibility.PUBLIC),
                Instant.parse("2026-03-28T00:00:00Z"),
                "최근 메시지",
                Instant.parse("2026-03-28T00:01:00Z")
        );
    }

    private DmThreadDetailResponse detailResponse() {
        return new DmThreadDetailResponse(
                10L,
                new DmParticipantResponse(2L, "miso", "미소", AccountVisibility.PUBLIC),
                Instant.parse("2026-03-28T00:00:00Z"),
                List.of(new DmMessageResponse(30L, 1L, "하루", false, null, "TEXT", null, null, "안녕!", Instant.parse("2026-03-28T00:01:00Z"))),
                List.of()
        );
    }
}
