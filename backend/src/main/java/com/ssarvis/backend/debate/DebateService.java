package com.ssarvis.backend.debate;

import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.CloneAccessPolicy;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.stream.NdjsonStreamWriter;
import com.ssarvis.backend.voice.RegisteredVoice;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
import com.ssarvis.backend.voice.VoiceAccessPolicy;
import com.ssarvis.backend.voice.VoiceService;
import com.ssarvis.backend.voice.VoiceSynthesisResult;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DebateService {
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final RegisteredVoiceRepository registeredVoiceRepository;
    private final DebateSessionRepository debateSessionRepository;
    private final DebateTurnRepository debateTurnRepository;
    private final VoiceService voiceService;
    private final VoiceAccessPolicy voiceAccessPolicy;
    private final OpenAiContextAssembler openAiContextAssembler;
    private final OpenAiClient openAiClient;
    private final AuthService authService;
    private final CloneAccessPolicy cloneAccessPolicy;

    public DebateService(
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository,
            RegisteredVoiceRepository registeredVoiceRepository,
            DebateSessionRepository debateSessionRepository,
            DebateTurnRepository debateTurnRepository,
            VoiceService voiceService,
            VoiceAccessPolicy voiceAccessPolicy,
            OpenAiContextAssembler openAiContextAssembler,
            OpenAiClient openAiClient,
            AuthService authService,
            CloneAccessPolicy cloneAccessPolicy
    ) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.debateSessionRepository = debateSessionRepository;
        this.debateTurnRepository = debateTurnRepository;
        this.voiceService = voiceService;
        this.voiceAccessPolicy = voiceAccessPolicy;
        this.openAiContextAssembler = openAiContextAssembler;
        this.openAiClient = openAiClient;
        this.authService = authService;
        this.cloneAccessPolicy = cloneAccessPolicy;
    }

    @Transactional
    public DebateProgressResponse startDebate(Long userId, DebateStartRequest request) {
        UserAccount user = authService.getActiveUserAccount(userId);
        DebateSession debateSession = createDebateSession(user, request);
        DebateTurnResponse firstTurn = createNextTurnInternal(userId, debateSession);
        return new DebateProgressResponse(debateSession.getId(), debateSession.getTopic(), firstTurn);
    }

    @Transactional
    public void streamStartDebate(Long userId, DebateStartRequest request, OutputStream outputStream) throws IOException {
        UserAccount user = authService.getActiveUserAccount(userId);
        DebateSession debateSession = createDebateSession(user, request);
        streamTurn(userId, debateSession, outputStream);
    }

    @Transactional
    public DebateProgressResponse createNextTurn(Long userId, Long debateSessionId) {
        authService.getActiveUserAccount(userId);
        DebateSession debateSession = debateSessionRepository.findByIdAndUserId(debateSessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Debate session not found."));

        DebateTurnResponse turn = createNextTurnInternal(userId, debateSession);
        return new DebateProgressResponse(debateSession.getId(), debateSession.getTopic(), turn);
    }

    @Transactional
    public void streamNextTurn(Long userId, Long debateSessionId, OutputStream outputStream) throws IOException {
        authService.getActiveUserAccount(userId);
        DebateSession debateSession = debateSessionRepository.findByIdAndUserId(debateSessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Debate session not found."));
        streamTurn(userId, debateSession, outputStream);
    }

    @Transactional(readOnly = true)
    public List<DebateSessionSummaryResponse> listDebates(Long userId) {
        authService.getActiveUserAccount(userId);
        return debateSessionRepository.findSummaryRowsByUserId(userId).stream()
                .map(summary -> new DebateSessionSummaryResponse(
                        summary.debateSessionId(),
                        summary.cloneAId(),
                        summary.cloneAAlias(),
                        summary.cloneBId(),
                        summary.cloneBAlias(),
                        summary.topic(),
                        summary.createdAt(),
                        Math.toIntExact(summary.turnCount())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DebateSessionDetailResponse getDebate(Long userId, Long debateSessionId) {
        authService.getActiveUserAccount(userId);
        DebateSession session = debateSessionRepository.findByIdAndUserId(debateSessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Debate session not found."));

        List<DebateHistoryTurnResponse> turns = debateTurnRepository.findByDebateSessionIdOrderByTurnIndexAsc(session.getId()).stream()
                .map(turn -> new DebateHistoryTurnResponse(
                        turn.getTurnIndex(),
                        turn.getSpeaker().name(),
                        turn.getSpeaker() == DebateTurn.Speaker.CLONE_A ? session.getCloneA().getId() : session.getCloneB().getId(),
                        turn.getContent(),
                        turn.getCreatedAt(),
                        turn.getAudioAsset() != null ? turn.getAudioAsset().getObjectUrl() : null,
                        turn.getAudioAsset() != null ? turn.getAudioAsset().getProviderVoiceId() : null
                ))
                .toList();

        return new DebateSessionDetailResponse(
                session.getId(),
                session.getCloneA().getId(),
                session.getCloneA().getAlias(),
                session.getCloneA().getShortDescription(),
                session.getCloneAVoice().getId(),
                session.getCloneB().getId(),
                session.getCloneB().getAlias(),
                session.getCloneB().getShortDescription(),
                session.getCloneBVoice().getId(),
                session.getTopic(),
                session.getCreatedAt(),
                turns
        );
    }

    private DebateSession createDebateSession(UserAccount user, DebateStartRequest request) {
        validateRequest(request);

        PromptGenerationLog cloneA = resolveUsableClone(user.getId(), request.cloneAId(), "Clone A not found.");
        PromptGenerationLog cloneB = resolveUsableClone(user.getId(), request.cloneBId(), "Clone B not found.");
        RegisteredVoice cloneAVoice = resolveUsableVoice(user.getId(), request.cloneAVoiceId(), "Clone A voice not found.");
        RegisteredVoice cloneBVoice = resolveUsableVoice(user.getId(), request.cloneBVoiceId(), "Clone B voice not found.");

        return debateSessionRepository.save(new DebateSession(
                user,
                cloneA,
                cloneB,
                cloneAVoice,
                cloneBVoice,
                request.topic().trim()
        ));
    }

    private void streamTurn(Long userId, DebateSession debateSession, OutputStream outputStream) throws IOException {
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
            ttsResult = voiceService.streamSynthesize(turnContext.message(), turnContext.activeVoice().getId(), userId, (base64Chunk, sampleRate, channels) -> writer.write(
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

    private DebateTurnResponse createNextTurnInternal(Long userId, DebateSession debateSession) {
        TurnContext turnContext = buildTurnContext(debateSession);
        VoiceSynthesisResult tts = voiceService.synthesize(turnContext.message(), turnContext.activeVoice().getId(), userId);

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

        String activeSpeakerName = speaker.name();
        List<OpenAiContextAssembler.DebateHistoryMessage> history = existingTurns.stream()
                .map(turn -> new OpenAiContextAssembler.DebateHistoryMessage(turn.getSpeaker().name(), turn.getContent()))
                .toList();

        String message = generateDebateTurn(activeClone.getSystemPrompt(), debateSession.getTopic(), activeSpeakerName, history);
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

    private String generateDebateTurn(
            String systemPrompt,
            String topic,
            String activeSpeakerName,
            List<OpenAiContextAssembler.DebateHistoryMessage> history
    ) {
        return openAiClient.requestChatCompletion(
                openAiContextAssembler.buildDebateMessages(
                        systemPrompt,
                        topic,
                        activeSpeakerName,
                        history,
                        appProperties.getOpenai().getChatHistoryTurns()
                )
        );
    }

    private PromptGenerationLog resolveUsableClone(Long userId, Long cloneId, String errorMessage) {
        try {
            return cloneAccessPolicy.getUsableClone(userId, cloneId);
        } catch (ResponseStatusException exception) {
            throw new ResponseStatusException(exception.getStatusCode(), errorMessage);
        }
    }

    private RegisteredVoice resolveUsableVoice(Long userId, Long voiceId, String errorMessage) {
        try {
            return voiceAccessPolicy.getUsableVoice(userId, voiceId);
        } catch (ResponseStatusException exception) {
            throw new ResponseStatusException(exception.getStatusCode(), errorMessage);
        }
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
