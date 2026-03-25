package com.ssarvis.backend.chat;

public record ChatResult(
        Long conversationId,
        String assistantMessage,
        String ttsVoiceId,
        String ttsAudioMimeType,
        String ttsAudioBase64
) {
}
