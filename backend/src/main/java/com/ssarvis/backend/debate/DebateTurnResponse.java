package com.ssarvis.backend.debate;

public record DebateTurnResponse(
        int turnIndex,
        String speaker,
        Long cloneId,
        String content,
        String ttsVoiceId,
        String ttsAudioMimeType,
        String ttsAudioBase64
) {
}
