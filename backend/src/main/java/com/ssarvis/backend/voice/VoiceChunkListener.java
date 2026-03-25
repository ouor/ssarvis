package com.ssarvis.backend.voice;

@FunctionalInterface
public interface VoiceChunkListener {
    void onAudioChunk(String base64PcmChunk) throws Exception;
}
