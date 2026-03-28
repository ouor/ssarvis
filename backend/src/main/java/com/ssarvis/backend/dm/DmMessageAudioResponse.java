package com.ssarvis.backend.dm;

public record DmMessageAudioResponse(
        Long messageId,
        String voiceId,
        String audioMimeType,
        String audioBase64
) {
}
