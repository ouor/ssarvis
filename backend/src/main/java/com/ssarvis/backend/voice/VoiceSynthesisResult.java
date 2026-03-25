package com.ssarvis.backend.voice;

public record VoiceSynthesisResult(
        String voiceId,
        String audioMimeType,
        String audioBase64,
        GeneratedAudioAsset audioAsset
) {
}
