package com.ssarvis.backend.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
            String audioUrl = requestTtsAudioUrl(limitTtsTextLength(text), voice.getProviderVoiceId(), dashscope);
            if (!StringUtils.hasText(audioUrl)) {
                return null;
            }

            HttpRequest audioRequest = HttpRequest.newBuilder()
                    .uri(URI.create(audioUrl))
                    .GET()
                    .build();
            HttpResponse<byte[]> audioResponse = httpClient.send(audioRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (audioResponse.statusCode() >= 400) {
                return null;
            }

            String audioMimeType = audioResponse.headers()
                    .firstValue("Content-Type")
                    .orElse("audio/wav");
            String audioBase64 = Base64.getEncoder().encodeToString(audioResponse.body());
            GeneratedAudioAsset audioAsset = audioStorageService.storeDashscopeAudio(
                    audioResponse.body(),
                    audioMimeType,
                    voice.getProviderVoiceId()
            );
            return new VoiceSynthesisResult(voice.getProviderVoiceId(), audioMimeType, audioBase64, audioAsset);
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
            String audioUrl = requestTtsAudioUrl(limitTtsTextLength(text), voice.getProviderVoiceId(), dashscope);
            if (!StringUtils.hasText(audioUrl)) {
                return null;
            }

            HttpRequest audioRequest = HttpRequest.newBuilder()
                    .uri(URI.create(audioUrl))
                    .GET()
                    .build();
            HttpResponse<InputStream> audioResponse = httpClient.send(audioRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (audioResponse.statusCode() >= 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "DashScope streamed audio download failed with status " + audioResponse.statusCode() + "."
                );
            }

            String audioMimeType = audioResponse.headers()
                    .firstValue("Content-Type")
                    .orElse("audio/wav");
            ByteArrayOutputStream fullAudioBytes = new ByteArrayOutputStream();

            try (InputStream inputStream = audioResponse.body()) {
                if (audioMimeType.contains("wav")) {
                    streamWavePcmChunks(inputStream, fullAudioBytes, chunkListener);
                } else {
                    fullAudioBytes.write(inputStream.readAllBytes());
                }
            }

            byte[] audioBytes = fullAudioBytes.toByteArray();
            GeneratedAudioAsset audioAsset = audioStorageService.storeDashscopeAudio(
                    audioBytes,
                    audioMimeType,
                    voice.getProviderVoiceId()
            );
            return new VoiceSynthesisResult(
                    voice.getProviderVoiceId(),
                    audioMimeType,
                    Base64.getEncoder().encodeToString(audioBytes),
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

    private void streamWavePcmChunks(
            InputStream inputStream,
            ByteArrayOutputStream fullAudioBytes,
            VoiceChunkListener chunkListener
    ) throws IOException {
        byte[] riffHeader = readExactBytes(inputStream, 12);
        if (riffHeader.length < 12) {
            fullAudioBytes.write(riffHeader);
            return;
        }

        fullAudioBytes.write(riffHeader);
        if (!"RIFF".equals(new String(riffHeader, 0, 4, StandardCharsets.US_ASCII))
                || !"WAVE".equals(new String(riffHeader, 8, 4, StandardCharsets.US_ASCII))) {
            fullAudioBytes.write(inputStream.readAllBytes());
            return;
        }

        while (true) {
            byte[] chunkHeader = readExactBytes(inputStream, 8);
            if (chunkHeader.length == 0) {
                return;
            }
            if (chunkHeader.length < 8) {
                fullAudioBytes.write(chunkHeader);
                return;
            }

            fullAudioBytes.write(chunkHeader);
            String chunkId = new String(chunkHeader, 0, 4, StandardCharsets.US_ASCII);
            int chunkSize = littleEndianInt(chunkHeader, 4);

            if ("data".equals(chunkId)) {
                streamExactChunkData(inputStream, fullAudioBytes, chunkListener, chunkSize);
                byte[] remaining = inputStream.readAllBytes();
                fullAudioBytes.write(remaining);
                return;
            }

            copyExactBytes(inputStream, fullAudioBytes, chunkSize);
            if ((chunkSize & 1) == 1) {
                copyExactBytes(inputStream, fullAudioBytes, 1);
            }
        }
    }

    private void streamExactChunkData(
            InputStream inputStream,
            ByteArrayOutputStream fullAudioBytes,
            VoiceChunkListener chunkListener,
            int chunkSize
    ) throws IOException {
        byte[] buffer = new byte[4096];
        int remaining = chunkSize;

        while (remaining > 0) {
            int bytesRead = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of WAV data chunk.");
            }

            fullAudioBytes.write(buffer, 0, bytesRead);
            if (chunkListener != null && bytesRead > 0) {
                byte[] pcmChunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, pcmChunk, 0, bytesRead);
                try {
                    chunkListener.onAudioChunk(Base64.getEncoder().encodeToString(pcmChunk));
                } catch (Exception exception) {
                    throw new IOException("Failed to forward audio chunk.", exception);
                }
            }

            remaining -= bytesRead;
        }

        if ((chunkSize & 1) == 1) {
            copyExactBytes(inputStream, fullAudioBytes, 1);
        }
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

    private String limitTtsTextLength(String text) {
        if (text.getBytes(StandardCharsets.UTF_8).length <= MAX_TTS_TEXT_LENGTH) {
            return text;
        }

        StringBuilder trimmed = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = trimmed.toString() + text.charAt(i);
            if (next.getBytes(StandardCharsets.UTF_8).length > MAX_TTS_TEXT_LENGTH) {
                break;
            }
            trimmed.append(text.charAt(i));
        }
        return trimmed.toString();
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
}
