package com.ssarvis.backend.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
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

    public VoiceService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            RegisteredVoiceRepository registeredVoiceRepository
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.registeredVoiceRepository = registeredVoiceRepository;
    }

    public RegisteredVoice registerVoice(MultipartFile sampleFile) {
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
            String preferredName = buildPreferredName(sampleFile.getOriginalFilename());
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
            return new VoiceSynthesisResult(voice.getProviderVoiceId(), audioMimeType, audioBase64);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to request DashScope TTS.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DashScope TTS request was interrupted.", exception);
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

    private String buildPreferredName(String originalFilename) {
        String baseName = StringUtils.hasText(originalFilename) ? originalFilename : "voice";
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
