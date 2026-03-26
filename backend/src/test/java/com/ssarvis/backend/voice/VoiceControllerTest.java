package com.ssarvis.backend.voice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(VoiceController.class)
@Import(GlobalExceptionHandler.class)
class VoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoiceService voiceService;

    @Test
    void registerVoiceReturnsRegisteredVoice() throws Exception {
        given(voiceService.registerVoice(eq(1L), any(), any()))
                .willReturn(new RegisteredVoice(
                        "voice-provider-1",
                        "qwen3-tts-vc-2026-01-22",
                        "sample-voice-1",
                        "차분한 민지",
                        "sample.mp3",
                        "audio/mpeg"
                ));

        MockMultipartFile sample = new MockMultipartFile("sample", "sample.mp3", "audio/mpeg", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/voices")
                        .file(sample)
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voiceId").value("voice-provider-1"))
                .andExpect(jsonPath("$.displayName").value("차분한 민지"))
                .andExpect(jsonPath("$.preferredName").value("sample-voice-1"))
                .andExpect(jsonPath("$.originalFilename").value("sample.mp3"))
                .andExpect(jsonPath("$.audioMimeType").value("audio/mpeg"));
    }

    @Test
    void registerVoicePropagatesErrors() throws Exception {
        given(voiceService.registerVoice(eq(1L), any(), any()))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Voice sample file is required."));

        MockMultipartFile sample = new MockMultipartFile("sample", "empty.mp3", "audio/mpeg", new byte[0]);

        mockMvc.perform(multipart("/api/voices")
                        .file(sample)
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Voice sample file is required."));
    }
}
