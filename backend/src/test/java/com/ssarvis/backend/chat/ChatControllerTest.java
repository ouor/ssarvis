package com.ssarvis.backend.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ChatController.class)
@Import(GlobalExceptionHandler.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void listConversationsReturnsUserOwnedHistory() throws Exception {
        given(chatService.listConversations(1L)).willReturn(List.of(
                new ChatConversationSummaryResponse(41L, 7L, "하루", Instant.parse("2026-03-26T01:00:00Z"), "마지막 메시지", 2)
        ));

        mockMvc.perform(get("/api/chat/conversations")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루", AccountVisibility.PUBLIC)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conversationId").value(41))
                .andExpect(jsonPath("$[0].cloneAlias").value("하루"))
                .andExpect(jsonPath("$[0].messageCount").value(2));
    }

    @Test
    void getConversationReturnsHistoryMessages() throws Exception {
        given(chatService.getConversation(1L, 41L)).willReturn(new ChatConversationDetailResponse(
                41L,
                7L,
                "하루",
                "차분한 클론",
                Instant.parse("2026-03-26T01:00:00Z"),
                List.of(
                        new ChatHistoryMessageResponse("user", "안녕", Instant.parse("2026-03-26T01:00:01Z"), null, null),
                        new ChatHistoryMessageResponse("assistant", "반가워요", Instant.parse("2026-03-26T01:00:02Z"), "https://cdn.example/audio.mp3", "voice-demo")
                )
        ));

        mockMvc.perform(get("/api/chat/conversations/41")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루", AccountVisibility.PUBLIC)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(41))
                .andExpect(jsonPath("$.messages[1].content").value("반가워요"))
                .andExpect(jsonPath("$.messages[1].ttsAudioUrl").value("https://cdn.example/audio.mp3"));
    }

    @Test
    void sendMessageReturnsAssistantMessage() throws Exception {
        given(chatService.reply(eq(1L), any()))
                .willReturn(new ChatResult(
                        41L,
                        "안녕하세요. 어떤 부분이 궁금한지 편하게 말씀해 주세요.",
                        "voice-demo",
                        "audio/wav",
                        "UklGRg=="
                ));

        mockMvc.perform(post("/api/chat/messages")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루", AccountVisibility.PUBLIC)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promptGenerationLogId": 7,
                                  "message": "자기소개를 해줘"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(41))
                .andExpect(jsonPath("$.assistantMessage").value("안녕하세요. 어떤 부분이 궁금한지 편하게 말씀해 주세요."))
                .andExpect(jsonPath("$.ttsVoiceId").value("voice-demo"))
                .andExpect(jsonPath("$.ttsAudioMimeType").value("audio/wav"))
                .andExpect(jsonPath("$.ttsAudioBase64").value("UklGRg=="));
    }

    @Test
    void sendMessageReturnsBadRequestWhenMessageIsBlank() throws Exception {
        mockMvc.perform(post("/api/chat/messages")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루", AccountVisibility.PUBLIC)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promptGenerationLogId": 7,
                                  "message": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.details[0]").value("message: message must not be blank."));
    }

    @Test
    void sendMessagePropagatesServiceErrors() throws Exception {
        given(chatService.reply(eq(1L), any()))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Conversation not found."));

        mockMvc.perform(post("/api/chat/messages")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루", AccountVisibility.PUBLIC)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": 999,
                                  "message": "이전 대화를 이어서 해줘"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Conversation not found."));
    }
}
