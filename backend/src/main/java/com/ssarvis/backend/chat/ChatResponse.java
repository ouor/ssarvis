package com.ssarvis.backend.chat;

public record ChatResponse(
        Long conversationId,
        String assistantMessage,
        String ttsVoiceId,
        String ttsAudioMimeType,
        String ttsAudioBase64
) {
}
