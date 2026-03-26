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
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoiceService {

    private static final int MAX_TTS_TEXT_LENGTH = 600;
    private static final long REALTIME_UPDATE_TIMEOUT_SECONDS = 20;
    private static final Duration EXTERNAL_REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final RegisteredVoiceRepository registeredVoiceRepository;
    private final AudioStorageService audioStorageService;
    private final AuthService authService;

    public VoiceService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            RegisteredVoiceRepository registeredVoiceRepository,
            AudioStorageService audioStorageService,
            AuthService authService
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.audioStorageService = audioStorageService;
        this.authService = authService;
    }

    public RegisteredVoice registerVoice(Long userId, MultipartFile sampleFile, String alias) {
        AppProperties.Dashscope dashscope = appProperties.getDashscope();
        UserAccount user = authService.getActiveUserAccount(userId);
        if (!StringUtils.hasText(dashscope.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DASHSCOPE_API_KEY is not configured.");
        }
        if (sampleFile == null || sampleFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voice sample file is required.");
        }
        if (!StringUtils.hasText(sampleFile.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voice sample content type is required.");
        }

        try {
            String displayName = buildDisplayName(alias, sampleFile.getOriginalFilename());
            String preferredName = buildPreferredName(displayName, sampleFile.getOriginalFilename());
            String dataUri = "data:" + sampleFile.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(sampleFile.getBytes());

            Map<String, Object> payload = Map.of(
                    "model", "qwen-voice-enrollment",
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

            return registeredVoiceRepository.save(new RegisteredVoice(
                    user,
                    providerVoiceId,
                    dashscope.getTtsModel(),
                    preferredName,
                    displayName,
                    sampleFile.getOriginalFilename() != null ? sampleFile.getOriginalFilename() : "voice-sample",
                    sampleFile.getContentType()
            ));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the uploaded voice sample.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DashScope voice registration was interrupted.", exception);
        }
    }

    public List<VoiceSummaryResponse> listVoices(Long userId) {
        authService.getActiveUserAccount(userId);
        String currentTtsModel = appProperties.getDashscope().getTtsModel();
        return registeredVoiceRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .filter(voice -> currentTtsModel.equals(voice.getTargetModel()))
                .map(voice -> new VoiceSummaryResponse(
                        voice.getId(),
                        voice.getProviderVoiceId(),
                        voice.getDisplayName(),
                        voice.getPreferredName(),
                        voice.getOriginalFilename(),
                        voice.getAudioMimeType(),
                        voice.getCreatedAt()
                ))
                .toList();
    }

    public VoiceSynthesisResult synthesize(String text, Long registeredVoiceId, Long userId) {
        if (!StringUtils.hasText(text) || registeredVoiceId == null) {
            return null;
        }

        AppProperties.Dashscope dashscope = appProperties.getDashscope();
        if (!StringUtils.hasText(dashscope.getApiKey())) {
            return null;
        }

        RegisteredVoice voice = registeredVoiceRepository.findByIdAndUserId(registeredVoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found."));
        validateVoiceCompatibility(voice);

        try {
            CombinedAudio combinedAudio = synthesizeCombinedAudio(text, voice.getProviderVoiceId(), dashscope, null);
            if (combinedAudio == null) {
                return null;
            }
            GeneratedAudioAsset audioAsset = audioStorageService.storeDashscopeAudio(
                    voice.getUser(),
                    combinedAudio.audioBytes(),
                    combinedAudio.audioMimeType(),
                    voice.getProviderVoiceId()
            );
            return new VoiceSynthesisResult(
                    voice.getProviderVoiceId(),
                    combinedAudio.audioMimeType(),
                    Base64.getEncoder().encodeToString(combinedAudio.audioBytes()),
                    audioAsset
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to request DashScope TTS.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DashScope TTS request was interrupted.", exception);
        }
    }

    public VoiceSynthesisResult streamSynthesize(String text, Long registeredVoiceId, Long userId, VoiceChunkListener chunkListener) {
        if (!StringUtils.hasText(text) || registeredVoiceId == null) {
            return null;
        }

        AppProperties.Dashscope dashscope = appProperties.getDashscope();
        if (!StringUtils.hasText(dashscope.getApiKey())) {
            return null;
        }

        RegisteredVoice voice = registeredVoiceRepository.findByIdAndUserId(registeredVoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found."));
        validateVoiceCompatibility(voice);

        try {
            CombinedAudio combinedAudio = synthesizeCombinedAudio(text, voice.getProviderVoiceId(), dashscope, chunkListener);
            if (combinedAudio == null) {
                return null;
            }
            GeneratedAudioAsset audioAsset = audioStorageService.storeDashscopeAudio(
                    voice.getUser(),
                    combinedAudio.audioBytes(),
                    combinedAudio.audioMimeType(),
                    voice.getProviderVoiceId()
            );
            return new VoiceSynthesisResult(
                    voice.getProviderVoiceId(),
                    combinedAudio.audioMimeType(),
                    Base64.getEncoder().encodeToString(combinedAudio.audioBytes()),
                    audioAsset
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process streamed TTS audio.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DashScope TTS stream was interrupted.", exception);
        }
    }

    private CombinedAudio synthesizeCombinedAudio(
            String text,
            String providerVoiceId,
            AppProperties.Dashscope dashscope,
            VoiceChunkListener chunkListener
    ) throws IOException, InterruptedException {
        List<String> segments = splitTextForTts(text);
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
                    .mode("server_commit")
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
            return new CombinedAudio(wrapPcmAsWav(pcmBytes, waveFormat), "audio/wav");
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

    private String buildDisplayName(String alias, String originalFilename) {
        if (StringUtils.hasText(alias)) {
            return alias.trim();
        }

        String baseName = StringUtils.hasText(originalFilename) ? originalFilename : "voice";
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = baseName.substring(0, extensionIndex);
        }
        return baseName;
    }

    private String buildPreferredName(String alias, String originalFilename) {
        String baseName = StringUtils.hasText(alias) ? alias : originalFilename;
        baseName = StringUtils.hasText(baseName) ? baseName : "voice";
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = baseName.substring(0, extensionIndex);
        }

        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z]", "")
                .toLowerCase();

        if (!StringUtils.hasText(normalized)) {
            normalized = "voice";
        }

        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
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

    private void validateVoiceCompatibility(RegisteredVoice voice) {
        String currentTtsModel = appProperties.getDashscope().getTtsModel();
        if (!currentTtsModel.equals(voice.getTargetModel())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This voice was registered for an older DashScope model. Please register it again."
            );
        }
    }

    private List<String> splitTextForTts(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        String remaining = normalized;

        while (StringUtils.hasText(remaining)) {
            if (utf8Length(remaining) <= MAX_TTS_TEXT_LENGTH) {
                chunks.add(remaining.trim());
                break;
            }

            int splitIndex = findLastPeriodWithinLimit(remaining);
            if (splitIndex < 0) {
                splitIndex = findLastWhitespaceWithinLimit(remaining);
            }
            if (splitIndex < 0) {
                splitIndex = findMaxCharBoundaryWithinLimit(remaining);
            }

            if (splitIndex <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Failed to split long TTS text within the provider byte limit."
                );
            }

            chunks.add(remaining.substring(0, splitIndex).trim());
            remaining = remaining.substring(splitIndex).trim();
        }

        return chunks.stream().filter(StringUtils::hasText).toList();
    }

    private int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private int findLastPeriodWithinLimit(String text) {
        int lastPeriodIndex = -1;
        int byteLength = 0;
        for (int index = 0; index < text.length(); index++) {
            byteLength += utf8Length(text.substring(index, index + 1));
            if (byteLength > MAX_TTS_TEXT_LENGTH) {
                break;
            }
            if (text.charAt(index) == '.') {
                lastPeriodIndex = index + 1;
            }
        }
        return lastPeriodIndex;
    }

    private int findLastWhitespaceWithinLimit(String text) {
        int lastWhitespaceIndex = -1;
        int byteLength = 0;
        for (int index = 0; index < text.length(); index++) {
            byteLength += utf8Length(text.substring(index, index + 1));
            if (byteLength > MAX_TTS_TEXT_LENGTH) {
                break;
            }
            if (Character.isWhitespace(text.charAt(index))) {
                lastWhitespaceIndex = index + 1;
            }
        }
        return lastWhitespaceIndex;
    }

    private int findMaxCharBoundaryWithinLimit(String text) {
        int lastValidIndex = -1;
        int byteLength = 0;
        for (int index = 0; index < text.length(); index++) {
            byteLength += utf8Length(text.substring(index, index + 1));
            if (byteLength > MAX_TTS_TEXT_LENGTH) {
                break;
            }
            lastValidIndex = index + 1;
        }
        return lastValidIndex;
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

    private record WaveFormat(int sampleRate, int channels, int bitsPerSample) {
    }

    private record CombinedAudio(byte[] audioBytes, String audioMimeType) {
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
