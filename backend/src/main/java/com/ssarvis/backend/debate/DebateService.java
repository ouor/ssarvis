package com.ssarvis.backend.debate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.RegisteredVoice;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
import com.ssarvis.backend.voice.VoiceService;
import com.ssarvis.backend.voice.VoiceSynthesisResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DebateService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final RegisteredVoiceRepository registeredVoiceRepository;
    private final DebateSessionRepository debateSessionRepository;
    private final DebateTurnRepository debateTurnRepository;
    private final VoiceService voiceService;

    public DebateService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository,
            RegisteredVoiceRepository registeredVoiceRepository,
            DebateSessionRepository debateSessionRepository,
            DebateTurnRepository debateTurnRepository,
            VoiceService voiceService
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.debateSessionRepository = debateSessionRepository;
        this.debateTurnRepository = debateTurnRepository;
        this.voiceService = voiceService;
    }

    @Transactional
    public DebateResponse debate(DebateRequest request) {
        validateRequest(request);

        PromptGenerationLog cloneA = promptGenerationLogRepository.findById(request.cloneAId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clone A not found."));
        PromptGenerationLog cloneB = promptGenerationLogRepository.findById(request.cloneBId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clone B not found."));
        RegisteredVoice cloneAVoice = registeredVoiceRepository.findById(request.cloneAVoiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clone A voice not found."));
        RegisteredVoice cloneBVoice = registeredVoiceRepository.findById(request.cloneBVoiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clone B voice not found."));

        DebateSession debateSession = debateSessionRepository.save(new DebateSession(
                cloneA,
                cloneB,
                cloneAVoice,
                cloneBVoice,
                request.topic().trim(),
                request.turnsPerClone()
        ));

        List<DebateTurnResponse> responses = new ArrayList<>();
        List<TranscriptEntry> transcript = new ArrayList<>();
        int totalTurns = request.turnsPerClone() * 2;

        for (int index = 0; index < totalTurns; index++) {
            boolean isCloneATurn = index % 2 == 0;
            PromptGenerationLog activeClone = isCloneATurn ? cloneA : cloneB;
            RegisteredVoice activeVoice = isCloneATurn ? cloneAVoice : cloneBVoice;
            DebateTurn.Speaker speaker = isCloneATurn ? DebateTurn.Speaker.CLONE_A : DebateTurn.Speaker.CLONE_B;
            String stance = isCloneATurn ? "찬성" : "반대";
            String message = generateDebateTurn(activeClone.getSystemPrompt(), request.topic().trim(), stance, transcript);

            debateTurnRepository.save(new DebateTurn(debateSession, speaker, index + 1, message));
            transcript.add(new TranscriptEntry(speaker, message));

            VoiceSynthesisResult tts = voiceService.synthesize(message, activeVoice.getId());
            responses.add(new DebateTurnResponse(
                    index + 1,
                    speaker.name(),
                    activeClone.getId(),
                    message,
                    tts != null ? tts.voiceId() : null,
                    tts != null ? tts.audioMimeType() : null,
                    tts != null ? tts.audioBase64() : null
            ));
        }

        return new DebateResponse(debateSession.getId(), debateSession.getTopic(), responses);
    }

    private void validateRequest(DebateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debate request is required.");
        }
        if (request.cloneAId() == null || request.cloneBId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cloneAId and cloneBId are required.");
        }
        if (request.cloneAId().equals(request.cloneBId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Two different clones must be selected.");
        }
        if (request.cloneAVoiceId() == null || request.cloneBVoiceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cloneAVoiceId and cloneBVoiceId are required.");
        }
        if (!StringUtils.hasText(request.topic())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topic must not be blank.");
        }
    }

    private String generateDebateTurn(String systemPrompt, String topic, String stance, List<TranscriptEntry> transcript) {
        String apiKey = appProperties.getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_API_KEY is not configured.");
        }

        StringBuilder transcriptText = new StringBuilder();
        for (TranscriptEntry entry : transcript) {
            transcriptText.append("- ").append(entry.speaker().name()).append(": ").append(entry.content()).append('\n');
        }

        String userPrompt = """
                너는 지금 토론 중인 클론이다.
                토론 주제: %s
                너의 입장: %s

                지금까지의 토론:
                %s

                요구사항:
                - 한국어로 답변
                - 자신의 입장을 분명하게 드러낼 것
                - 상대 발언에 직접 반박하거나 응답할 것
                - 2~4문단 또는 3~5문장 정도의 분량
                - 토론체이되 과도하게 공격적이지 않을 것
                - 메타 설명 없이 바로 발언만 출력
                """.formatted(topic, stance, transcriptText.length() > 0 ? transcriptText : "(아직 발언 없음)\n");

        try {
            String payload = objectMapper.writeValueAsString(new OpenAiRequest(
                    appProperties.getOpenai().getModel(),
                    new Reasoning("low"),
                    new Text("medium"),
                    List.of(
                            new InputMessage("developer", systemPrompt),
                            new InputMessage("user", userPrompt)
                    )
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getOpenai().getBaseUrl() + "/responses"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI request failed with status " + response.statusCode() + ". Body: " + abbreviate(response.body(), 400)
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            String output = extractOutputText(root);
            if (!StringUtils.hasText(output)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include output_text.");
            }
            return output.trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI request was interrupted.", exception);
        }
    }

    private String extractOutputText(JsonNode root) {
        JsonNode directOutputText = root.get("output_text");
        if (directOutputText != null && directOutputText.isTextual() && StringUtils.hasText(directOutputText.asText())) {
            return directOutputText.asText();
        }

        JsonNode output = root.get("output");
        if (output == null || !output.isArray()) {
            return "";
        }

        StringBuilder collected = new StringBuilder();
        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }
            JsonNode content = item.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if (!"output_text".equals(contentItem.path("type").asText())) {
                    continue;
                }
                String text = contentItem.path("text").asText("");
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                if (!collected.isEmpty()) {
                    collected.append('\n');
                }
                collected.append(text);
            }
        }

        return collected.toString();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private record TranscriptEntry(DebateTurn.Speaker speaker, String content) {
    }

    private record OpenAiRequest(
            String model,
            Reasoning reasoning,
            Text text,
            List<InputMessage> input
    ) {
    }

    private record Reasoning(String effort) {
    }

    private record Text(String verbosity) {
    }

    private record InputMessage(String role, String content) {
    }
}
