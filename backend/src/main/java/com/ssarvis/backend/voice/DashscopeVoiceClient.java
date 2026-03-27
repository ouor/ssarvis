package com.ssarvis.backend.voice;

import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtime;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeAudioFormat;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeCallback;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeConfig;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.ssarvis.backend.config.AppProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DashscopeVoiceClient {

    private static final String VOICE_ENROLLMENT_MODEL = "qwen-voice-enrollment";
    private static final String REALTIME_MODE = "server_commit";
    private static final String AUDIO_MIME_TYPE = "audio/wav";
    private static final Duration EXTERNAL_REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final long REALTIME_UPDATE_TIMEOUT_SECONDS = 20;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TtsTextSplitter ttsTextSplitter;

    public DashscopeVoiceClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            TtsTextSplitter ttsTextSplitter
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.ttsTextSplitter = ttsTextSplitter;
    }

    public String registerVoice(
            AppProperties.Dashscope dashscope,
            MultipartFile sampleFile,
            String preferredName
    ) throws IOException, InterruptedException {
        String dataUri = "data:" + sampleFile.getContentType() + ";base64,"
                + Base64.getEncoder().encodeToString(sampleFile.getBytes());

        Map<String, Object> payload = Map.of(
                "model", VOICE_ENROLLMENT_MODEL,
                "input", Map.of(
                        "action", "create",
                        "target_model", dashscope.getTtsModel(),
                        "preferred_name", preferredName,
                        "audio", Map.of("data", dataUri)
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dashscope.getBaseUrl() + "/services/audio/tts/customization"))
                .timeout(EXTERNAL_REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + dashscope.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "DashScope voice registration failed with status " + response.statusCode() + ". Body: " + abbreviate(response.body(), 400)
            );
        }

        JsonNode root = objectMapper.readTree(response.body());
        String providerVoiceId = root.path("output").path("voice").asText("");
        if (!StringUtils.hasText(providerVoiceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DashScope voice registration did not return a voice id.");
        }
        return providerVoiceId;
    }

    public DashscopeSynthesisResult synthesize(
            String text,
            String providerVoiceId,
            AppProperties.Dashscope dashscope,
            VoiceChunkListener chunkListener
    ) throws IOException, InterruptedException {
        List<String> segments = ttsTextSplitter.split(text);
        if (segments.isEmpty()) {
            return null;
        }

        RealtimeSynthesisCollector collector = new RealtimeSynthesisCollector(chunkListener);
        QwenTtsRealtime realtimeClient = buildRealtimeClient(dashscope, collector);

        try {
            connectRealtimeClient(realtimeClient);
            realtimeClient.updateSession(QwenTtsRealtimeConfig.builder()
                    .voice(providerVoiceId)
                    .responseFormat(QwenTtsRealtimeAudioFormat.PCM_24000HZ_MONO_16BIT)
                    .mode(REALTIME_MODE)
                    .build());

            for (String segment : segments) {
                realtimeClient.appendText(segment);
            }
            realtimeClient.finish();

            collector.awaitFinished();
            collector.throwIfFailed();

            byte[] pcmBytes = collector.toPcmBytes();
            if (pcmBytes.length == 0) {
                return null;
            }

            WaveFormat waveFormat = new WaveFormat(collector.getSampleRate(), collector.getChannels(), 16);
            return new DashscopeSynthesisResult(wrapPcmAsWav(pcmBytes, waveFormat), AUDIO_MIME_TYPE);
        } catch (NoApiKeyException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DASHSCOPE_API_KEY is not configured.", exception);
        } finally {
            try {
                realtimeClient.close();
            } catch (Exception ignored) {
                // The session may already be closed after finish().
            }
        }
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private byte[] wrapPcmAsWav(byte[] pcmBytes, WaveFormat format) {
        int headerSize = 44;
        byte[] wavBytes = new byte[headerSize + pcmBytes.length];
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(wavBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        writeAscii(wavBytes, 0, "RIFF");
        buffer.putInt(4, 36 + pcmBytes.length);
        writeAscii(wavBytes, 8, "WAVE");
        writeAscii(wavBytes, 12, "fmt ");
        buffer.putInt(16, 16);
        buffer.putShort(20, (short) 1);
        buffer.putShort(22, (short) format.channels());
        buffer.putInt(24, format.sampleRate());
        buffer.putInt(28, format.sampleRate() * format.channels() * (format.bitsPerSample() / 8));
        buffer.putShort(32, (short) (format.channels() * (format.bitsPerSample() / 8)));
        buffer.putShort(34, (short) format.bitsPerSample());
        writeAscii(wavBytes, 36, "data");
        buffer.putInt(40, pcmBytes.length);
        System.arraycopy(pcmBytes, 0, wavBytes, headerSize, pcmBytes.length);
        return wavBytes;
    }

    private void writeAscii(byte[] target, int offset, String value) {
        for (int index = 0; index < value.length(); index++) {
            target[offset + index] = (byte) value.charAt(index);
        }
    }

    private QwenTtsRealtime buildRealtimeClient(AppProperties.Dashscope dashscope, RealtimeSynthesisCollector collector) {
        QwenTtsRealtimeParam param = QwenTtsRealtimeParam.builder()
                .model(dashscope.getTtsModel())
                .url(dashscope.getRealtimeUrl())
                .apikey(dashscope.getApiKey())
                .build();
        return new QwenTtsRealtime(param, collector);
    }

    private void connectRealtimeClient(QwenTtsRealtime realtimeClient) throws NoApiKeyException {
        CompletableFuture<Void> connectFuture = CompletableFuture.runAsync(() -> {
            try {
                realtimeClient.connect();
            } catch (NoApiKeyException exception) {
                throw new RealtimeConnectException(exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RealtimeConnectionInterruptedException(exception);
            }
        });

        try {
            connectFuture.get(REALTIME_UPDATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RealtimeConnectException realtimeConnectException) {
                throw realtimeConnectException.getCause();
            }
            if (cause instanceof RealtimeConnectionInterruptedException realtimeConnectionInterruptedException) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "DashScope realtime TTS connection was interrupted.",
                        realtimeConnectionInterruptedException.getCause()
                );
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DashScope realtime TTS connection failed.", cause);
        } catch (TimeoutException exception) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "DashScope realtime TTS connection timed out.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DashScope realtime TTS connection was interrupted.", exception);
        }
    }

    record DashscopeSynthesisResult(byte[] audioBytes, String audioMimeType) {
    }

    private record WaveFormat(int sampleRate, int channels, int bitsPerSample) {
    }

    private static final class RealtimeSynthesisCollector extends QwenTtsRealtimeCallback {
        private final ByteArrayOutputStream pcmBytes = new ByteArrayOutputStream();
        private final CountDownLatch finishedLatch = new CountDownLatch(1);
        private final AtomicReference<RuntimeException> callbackFailure = new AtomicReference<>();
        private final VoiceChunkListener chunkListener;
        private volatile long lastUpdateAtMillis = System.currentTimeMillis();
        private volatile int sampleRate = 24000;
        private volatile int channels = 1;

        private RealtimeSynthesisCollector(VoiceChunkListener chunkListener) {
            this.chunkListener = chunkListener;
        }

        @Override
        public void onOpen() {
            touch();
        }

        @Override
        public void onEvent(JsonObject message) {
            try {
                touch();
                String type = message.has("type") ? message.get("type").getAsString() : "";
                switch (type) {
                    case "response.audio.delta" -> handleAudioDelta(message);
                    case "session.finished" -> finishedLatch.countDown();
                    case "error", "response.error" -> fail(extractErrorMessage(message));
                    default -> {
                    }
                }
            } catch (Exception exception) {
                fail("Failed to process DashScope realtime event.", exception);
            }
        }

        @Override
        public void onClose(int code, String reason) {
            touch();
            if (code >= 4000 && callbackFailure.get() == null) {
                fail("DashScope realtime connection closed unexpectedly: " + code + " " + reason);
                return;
            }
            finishedLatch.countDown();
        }

        private void handleAudioDelta(JsonObject message) throws Exception {
            if (!message.has("delta")) {
                return;
            }

            byte[] chunk = Base64.getDecoder().decode(message.get("delta").getAsString());
            synchronized (pcmBytes) {
                pcmBytes.write(chunk);
            }
            if (chunkListener != null) {
                chunkListener.onAudioChunk(Base64.getEncoder().encodeToString(chunk), sampleRate, channels);
            }
        }

        private void awaitFinished() throws InterruptedException {
            while (!finishedLatch.await(1, TimeUnit.SECONDS)) {
                long idleMillis = System.currentTimeMillis() - lastUpdateAtMillis;
                if (idleMillis >= TimeUnit.SECONDS.toMillis(REALTIME_UPDATE_TIMEOUT_SECONDS)) {
                    throw new ResponseStatusException(
                            HttpStatus.GATEWAY_TIMEOUT,
                            "DashScope realtime TTS did not receive updates for 20 seconds."
                    );
                }
            }
        }

        private void throwIfFailed() {
            RuntimeException failure = callbackFailure.get();
            if (failure != null) {
                throw failure;
            }
        }

        private byte[] toPcmBytes() {
            synchronized (pcmBytes) {
                return pcmBytes.toByteArray();
            }
        }

        private int getSampleRate() {
            return sampleRate;
        }

        private int getChannels() {
            return channels;
        }

        private void touch() {
            lastUpdateAtMillis = System.currentTimeMillis();
        }

        private void fail(String message) {
            fail(message, null);
        }

        private void fail(String message, Exception cause) {
            callbackFailure.compareAndSet(
                    null,
                    new ResponseStatusException(HttpStatus.BAD_GATEWAY, message, cause)
            );
            finishedLatch.countDown();
        }

        private String extractErrorMessage(JsonObject message) {
            if (message.has("error")) {
                JsonObject error = message.getAsJsonObject("error");
                if (error.has("message")) {
                    return "DashScope realtime TTS failed: " + error.get("message").getAsString();
                }
            }
            if (message.has("message")) {
                return "DashScope realtime TTS failed: " + message.get("message").getAsString();
            }
            return "DashScope realtime TTS failed.";
        }
    }

    private static final class RealtimeConnectException extends RuntimeException {
        private RealtimeConnectException(NoApiKeyException cause) {
            super(cause);
        }

        @Override
        public NoApiKeyException getCause() {
            return (NoApiKeyException) super.getCause();
        }
    }

    private static final class RealtimeConnectionInterruptedException extends RuntimeException {
        private RealtimeConnectionInterruptedException(InterruptedException cause) {
            super(cause);
        }

        @Override
        public InterruptedException getCause() {
            return (InterruptedException) super.getCause();
        }
    }
}
