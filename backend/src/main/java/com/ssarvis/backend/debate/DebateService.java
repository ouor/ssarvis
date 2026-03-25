package com.ssarvis.backend.debate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiChatCompletionRequest;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.stream.NdjsonStreamWriter;
import com.ssarvis.backend.voice.RegisteredVoice;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
import com.ssarvis.backend.voice.VoiceService;
import com.ssarvis.backend.voice.VoiceSynthesisResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
    private final OpenAiContextAssembler openAiContextAssembler;

    public DebateService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository,
            RegisteredVoiceRepository registeredVoiceRepository,
            DebateSessionRepository debateSessionRepository,
            DebateTurnRepository debateTurnRepository,
            VoiceService voiceService,
            OpenAiContextAssembler openAiContextAssembler
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.debateSessionRepository = debateSessionRepository;
        this.debateTurnRepository = debateTurnRepository;
        this.voiceService = voiceService;
        this.openAiContextAssembler = openAiContextAssembler;
    }

    @Transactional
    public DebateProgressResponse startDebate(DebateStartRequest request) {
        DebateSession debateSession = createDebateSession(request);
        DebateTurnResponse firstTurn = createNextTurnInternal(debateSession);
        return new DebateProgressResponse(debateSession.getId(), debateSession.getTopic(), firstTurn);
    }

    @Transactional
    public void streamStartDebate(DebateStartRequest request, OutputStream outputStream) throws IOException {
        DebateSession debateSession = createDebateSession(request);
        streamTurn(debateSession, outputStream);
    }

    @Transactional
    public DebateProgressResponse createNextTurn(Long debateSessionId) {
        DebateSession debateSession = debateSessionRepository.findById(debateSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Debate session not found."));

        DebateTurnResponse turn = createNextTurnInternal(debateSession);
        return new DebateProgressResponse(debateSession.getId(), debateSession.getTopic(), turn);
    }

    @Transactional
    public void streamNextTurn(Long debateSessionId, OutputStream outputStream) throws IOException {
        DebateSession debateSession = debateSessionRepository.findById(debateSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Debate session not found."));
        streamTurn(debateSession, outputStream);
    }

    @Transactional
    public void stopDebate(Long debateSessionId) {
        if (!debateSessionRepository.existsById(debateSessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Debate session not found.");
        }
    }

    private DebateSession createDebateSession(DebateStartRequest request) {
        validateRequest(request);

        PromptGenerationLog cloneA = promptGenerationLogRepository.findById(request.cloneAId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clone A not found."));
        PromptGenerationLog cloneB = promptGenerationLogRepository.findById(request.cloneBId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clone B not found."));
        RegisteredVoice cloneAVoice = registeredVoiceRepository.findById(request.cloneAVoiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clone A voice not found."));
        RegisteredVoice cloneBVoice = registeredVoiceRepository.findById(request.cloneBVoiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clone B voice not found."));

        return debateSessionRepository.save(new DebateSession(
                cloneA,
                cloneB,
                cloneAVoice,
                cloneBVoice,
                request.topic().trim()
        ));
    }

    private void streamTurn(DebateSession debateSession, OutputStream outputStream) throws IOException {
        NdjsonStreamWriter writer = new NdjsonStreamWriter(outputStream, objectMapper);
        TurnContext turnContext = buildTurnContext(debateSession);

        writer.write(Map.of(
                "type", "turn",
                "debateSessionId", debateSession.getId(),
                "topic", debateSession.getTopic(),
                "turn", Map.of(
                        "turnIndex", turnContext.turnIndex(),
                        "speaker", turnContext.speaker().name(),
                        "cloneId", turnContext.activeClone().getId(),
                        "content", turnContext.message()
                )
        ));

        VoiceSynthesisResult ttsResult = null;
        try {
            ttsResult = voiceService.streamSynthesize(turnContext.message(), turnContext.activeVoice().getId(), (base64Chunk, sampleRate, channels) -> writer.write(
                    Map.of(
                            "type", "audio_chunk",
                            "audioFormat", "pcm_s16le",
                            "sampleRate", sampleRate,
                            "channels", channels,
                            "chunkBase64", base64Chunk
                    )
            ));
        } catch (ResponseStatusException exception) {
            writer.writeError(exception.getReason() != null ? exception.getReason() : "Failed to stream TTS audio.");
        } catch (Exception exception) {
            writer.writeError("Failed to stream TTS audio.");
        }

        debateTurnRepository.save(new DebateTurn(
                debateSession,
                turnContext.speaker(),
                turnContext.turnIndex(),
                turnContext.message(),
                ttsResult != null ? ttsResult.audioAsset() : null
        ));

        writer.write(Map.of(
                "type", "done",
                "debateSessionId", debateSession.getId(),
                "turnIndex", turnContext.turnIndex(),
                "ttsVoiceId", ttsResult != null ? ttsResult.voiceId() : "",
                "hasAudio", ttsResult != null
        ));
    }

    private DebateTurnResponse createNextTurnInternal(DebateSession debateSession) {
        TurnContext turnContext = buildTurnContext(debateSession);
        VoiceSynthesisResult tts = voiceService.synthesize(turnContext.message(), turnContext.activeVoice().getId());

        debateTurnRepository.save(new DebateTurn(
                debateSession,
                turnContext.speaker(),
                turnContext.turnIndex(),
                turnContext.message(),
                tts != null ? tts.audioAsset() : null
        ));

        return new DebateTurnResponse(
                turnContext.turnIndex(),
                turnContext.speaker().name(),
                turnContext.activeClone().getId(),
                turnContext.message(),
                tts != null ? tts.voiceId() : null,
                tts != null ? tts.audioMimeType() : null,
                tts != null ? tts.audioBase64() : null
        );
    }

    private TurnContext buildTurnContext(DebateSession debateSession) {
        List<DebateTurn> existingTurns = debateTurnRepository.findByDebateSessionIdOrderByTurnIndexAsc(debateSession.getId());
        boolean isCloneATurn = existingTurns.size() % 2 == 0;
        PromptGenerationLog activeClone = isCloneATurn ? debateSession.getCloneA() : debateSession.getCloneB();
        RegisteredVoice activeVoice = isCloneATurn ? debateSession.getCloneAVoice() : debateSession.getCloneBVoice();
        DebateTurn.Speaker speaker = isCloneATurn ? DebateTurn.Speaker.CLONE_A : DebateTurn.Speaker.CLONE_B;
        String stance = isCloneATurn ? "찬성" : "반대";

        List<TranscriptEntry> transcript = existingTurns.stream()
                .map(turn -> new TranscriptEntry(turn.getSpeaker(), turn.getContent()))
                .toList();

        String message = generateDebateTurn(activeClone.getSystemPrompt(), debateSession.getTopic(), stance, transcript);
        return new TurnContext(activeClone, activeVoice, speaker, existingTurns.size() + 1, message);
    }

    private void validateRequest(DebateStartRequest request) {
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

        try {
            String payload = objectMapper.writeValueAsString(new OpenAiChatCompletionRequest(
                    appProperties.getOpenai().getModel(),
                    openAiContextAssembler.buildDebateMessages(
                            systemPrompt,
                            topic,
                            stance,
                            transcript.stream().map(entry -> "- " + entry.speaker().name() + ": " + entry.content()).toList()
                    )
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getOpenai().getBaseUrl() + "/chat/completions"))
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
            String output = extractAssistantMessage(root);
            if (!StringUtils.hasText(output)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include assistant content.");
            }
            return output.trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI request was interrupted.", exception);
        }
    }

    private String extractAssistantMessage(JsonNode root) {
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return "";
        }

        JsonNode contentNode = choices.get(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }

        return "";
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private record TranscriptEntry(DebateTurn.Speaker speaker, String content) {
    }

    private record TurnContext(
            PromptGenerationLog activeClone,
            RegisteredVoice activeVoice,
            DebateTurn.Speaker speaker,
            int turnIndex,
            String message
    ) {
    }
}
