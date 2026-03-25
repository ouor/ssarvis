package com.ssarvis.backend.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoiceService {

    private static final String LANGUAGE_TYPE = "Auto";
    private static final int MAX_TTS_TEXT_LENGTH = 600;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final RegisteredVoiceRepository registeredVoiceRepository;
    private final AudioStorageService audioStorageService;

    public VoiceService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            RegisteredVoiceRepository registeredVoiceRepository,
            AudioStorageService audioStorageService
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.audioStorageService = audioStorageService;
    }

    public RegisteredVoice registerVoice(MultipartFile sampleFile, String alias) {
        AppProperties.Dashscope dashscope = appProperties.getDashscope();
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

    public List<VoiceSummaryResponse> listVoices() {
        return registeredVoiceRepository.findAllByOrderByIdDesc().stream()
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

    public VoiceSynthesisResult synthesize(String text, Long registeredVoiceId) {
        if (!StringUtils.hasText(text) || registeredVoiceId == null) {
            return null;
        }

        AppProperties.Dashscope dashscope = appProperties.getDashscope();
        if (!StringUtils.hasText(dashscope.getApiKey())) {
            return null;
        }

        RegisteredVoice voice = registeredVoiceRepository.findById(registeredVoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found."));

        try {
            CombinedAudio combinedAudio = synthesizeCombinedAudio(text, voice.getProviderVoiceId(), dashscope, null);
            if (combinedAudio == null) {
                return null;
            }
            GeneratedAudioAsset audioAsset = audioStorageService.storeDashscopeAudio(
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

    public VoiceSynthesisResult streamSynthesize(String text, Long registeredVoiceId, VoiceChunkListener chunkListener) {
        if (!StringUtils.hasText(text) || registeredVoiceId == null) {
            return null;
        }

        AppProperties.Dashscope dashscope = appProperties.getDashscope();
        if (!StringUtils.hasText(dashscope.getApiKey())) {
            return null;
        }

        RegisteredVoice voice = registeredVoiceRepository.findById(registeredVoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found."));

        try {
            CombinedAudio combinedAudio = synthesizeCombinedAudio(text, voice.getProviderVoiceId(), dashscope, chunkListener);
            if (combinedAudio == null) {
                return null;
            }
            GeneratedAudioAsset audioAsset = audioStorageService.storeDashscopeAudio(
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

    private String requestTtsAudioUrl(String text, String voiceId, AppProperties.Dashscope dashscope)
            throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
                "model", dashscope.getTtsModel(),
                "input", Map.of(
                        "text", text,
                        "voice", voiceId,
                        "language_type", LANGUAGE_TYPE
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dashscope.getBaseUrl() + "/services/aigc/multimodal-generation/generation"))
                .header("Authorization", "Bearer " + dashscope.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "DashScope TTS failed with status " + response.statusCode() + ". Body: " + abbreviate(response.body(), 400)
            );
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("output").path("audio").path("url").asText("");
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

        ByteArrayOutputStream combinedPcmBytes = new ByteArrayOutputStream();
        WaveFormat waveFormat = null;
        String audioMimeType = "audio/wav";

        for (String segment : segments) {
            String audioUrl = requestTtsAudioUrl(segment, providerVoiceId, dashscope);
            if (!StringUtils.hasText(audioUrl)) {
                continue;
            }

            HttpRequest audioRequest = HttpRequest.newBuilder()
                    .uri(URI.create(audioUrl))
                    .GET()
                    .build();
            HttpResponse<byte[]> audioResponse = httpClient.send(audioRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (audioResponse.statusCode() >= 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "DashScope audio download failed with status " + audioResponse.statusCode() + "."
                );
            }

            audioMimeType = audioResponse.headers().firstValue("Content-Type").orElse("audio/wav");
            byte[] audioBytes = audioResponse.body();

            if (!audioMimeType.contains("wav")) {
                if (segments.size() > 1) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "DashScope returned unsupported audio type for segmented TTS: " + audioMimeType
                    );
                }
                return new CombinedAudio(audioBytes, audioMimeType);
            }

            ParsedWaveAudio parsedWaveAudio = parseWaveAudio(audioBytes);
            if (waveFormat == null) {
                waveFormat = parsedWaveAudio.format();
            } else if (!waveFormat.matches(parsedWaveAudio.format())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DashScope returned inconsistent WAV formats across TTS segments.");
            }

            combinedPcmBytes.write(parsedWaveAudio.pcmBytes());
            if (chunkListener != null) {
                streamPcmBytes(parsedWaveAudio.pcmBytes(), parsedWaveAudio.format(), chunkListener);
            }
        }

        if (combinedPcmBytes.size() == 0) {
            return null;
        }

        WaveFormat finalFormat = waveFormat != null ? waveFormat : new WaveFormat(24000, 1, 16);
        return new CombinedAudio(wrapPcmAsWav(combinedPcmBytes.toByteArray(), finalFormat), "audio/wav");
    }

    private byte[] readExactBytes(InputStream inputStream, int expectedLength) throws IOException {
        byte[] buffer = new byte[expectedLength];
        int offset = 0;

        while (offset < expectedLength) {
            int bytesRead = inputStream.read(buffer, offset, expectedLength - offset);
            if (bytesRead == -1) {
                break;
            }
            offset += bytesRead;
        }

        if (offset == expectedLength) {
            return buffer;
        }

        byte[] partial = new byte[offset];
        System.arraycopy(buffer, 0, partial, 0, offset);
        return partial;
    }

    private void copyExactBytes(InputStream inputStream, ByteArrayOutputStream outputStream, int length) throws IOException {
        byte[] buffer = new byte[4096];
        int remaining = length;

        while (remaining > 0) {
            int bytesRead = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of WAV chunk.");
            }
            outputStream.write(buffer, 0, bytesRead);
            remaining -= bytesRead;
        }
    }

    private int littleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 3] & 0xff) << 24);
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

    private ParsedWaveAudio parseWaveAudio(byte[] audioBytes) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(audioBytes)) {
            byte[] riffHeader = readExactBytes(inputStream, 12);
            if (riffHeader.length < 12
                    || !"RIFF".equals(new String(riffHeader, 0, 4, StandardCharsets.US_ASCII))
                    || !"WAVE".equals(new String(riffHeader, 8, 4, StandardCharsets.US_ASCII))) {
                throw new IOException("Unsupported WAV header.");
            }

            WaveFormat format = null;
            byte[] pcmBytes = new byte[0];

            while (true) {
                byte[] chunkHeader = readExactBytes(inputStream, 8);
                if (chunkHeader.length == 0) {
                    break;
                }
                if (chunkHeader.length < 8) {
                    throw new IOException("Unexpected end of WAV chunk header.");
                }

                String chunkId = new String(chunkHeader, 0, 4, StandardCharsets.US_ASCII);
                int chunkSize = littleEndianInt(chunkHeader, 4);
                byte[] chunkData = readExactBytes(inputStream, chunkSize);
                if (chunkData.length < chunkSize) {
                    throw new IOException("Unexpected end of WAV chunk data.");
                }

                if ("fmt ".equals(chunkId)) {
                    format = parseWaveFormat(chunkData);
                } else if ("data".equals(chunkId)) {
                    pcmBytes = chunkData;
                }

                if ((chunkSize & 1) == 1) {
                    readExactBytes(inputStream, 1);
                }
            }

            if (format == null) {
                format = new WaveFormat(24000, 1, 16);
            }

            return new ParsedWaveAudio(format, pcmBytes);
        }
    }

    private WaveFormat parseWaveFormat(byte[] chunkData) throws IOException {
        if (chunkData.length < 16) {
            throw new IOException("Invalid WAV fmt chunk.");
        }

        int audioFormat = littleEndianShort(chunkData, 0);
        int channels = littleEndianShort(chunkData, 2);
        int sampleRate = littleEndianInt(chunkData, 4);
        int bitsPerSample = littleEndianShort(chunkData, 14);

        if (audioFormat != 1) {
            throw new IOException("Only PCM WAV is supported.");
        }

        return new WaveFormat(sampleRate, channels, bitsPerSample);
    }

    private int littleEndianShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    private void streamPcmBytes(byte[] pcmBytes, WaveFormat format, VoiceChunkListener chunkListener) throws IOException {
        int offset = 0;
        while (offset < pcmBytes.length) {
            int length = Math.min(4096, pcmBytes.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(pcmBytes, offset, chunk, 0, length);
            try {
                chunkListener.onAudioChunk(
                        Base64.getEncoder().encodeToString(chunk),
                        format.sampleRate(),
                        format.channels()
                );
            } catch (Exception exception) {
                throw new IOException("Failed to forward audio chunk.", exception);
            }
            offset += length;
        }
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

    private record WaveFormat(int sampleRate, int channels, int bitsPerSample) {
        private boolean matches(WaveFormat other) {
            return other != null
                    && sampleRate == other.sampleRate
                    && channels == other.channels
                    && bitsPerSample == other.bitsPerSample;
        }
    }

    private record ParsedWaveAudio(WaveFormat format, byte[] pcmBytes) {
    }

    private record CombinedAudio(byte[] audioBytes, String audioMimeType) {
    }
}
