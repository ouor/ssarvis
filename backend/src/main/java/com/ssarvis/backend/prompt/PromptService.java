package com.ssarvis.backend.prompt;

import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.access.AssetListScope;
import com.ssarvis.backend.access.VisibilityUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.friend.FriendRelationshipService;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromptService {
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final OpenAiContextAssembler openAiContextAssembler;
    private final OpenAiClient openAiClient;
    private final AuthService authService;
    private final CloneAccessPolicy cloneAccessPolicy;
    private final FriendRelationshipService friendRelationshipService;

    public PromptService(
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository,
            OpenAiContextAssembler openAiContextAssembler,
            OpenAiClient openAiClient,
            AuthService authService,
            CloneAccessPolicy cloneAccessPolicy,
            FriendRelationshipService friendRelationshipService
    ) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.openAiContextAssembler = openAiContextAssembler;
        this.openAiClient = openAiClient;
        this.authService = authService;
        this.cloneAccessPolicy = cloneAccessPolicy;
        this.friendRelationshipService = friendRelationshipService;
    }

    public PromptGenerateResult generateSystemPrompt(Long userId, PromptGenerateRequest request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one answer is required.");
        }
        UserAccount user = authService.getActiveUserAccount(userId);

        try {
            String systemPrompt = normalizeSystemPrompt(openAiClient.requestChatCompletion(
                    openAiContextAssembler.buildSystemPromptGenerationMessages(request.answers())
            ));
            if (!StringUtils.hasText(systemPrompt)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include system prompt content.");
            }

            String alias = normalizeAlias(openAiClient.requestChatCompletion(
                    openAiContextAssembler.buildAliasGenerationMessages(systemPrompt)
            ));
            if (!StringUtils.hasText(alias)) {
                alias = "새 클론";
            }

            String shortDescription = normalizeShortDescription(openAiClient.requestChatCompletion(
                    openAiContextAssembler.buildShortDescriptionGenerationMessages(systemPrompt)
            ));
            if (!StringUtils.hasText(shortDescription)) {
                shortDescription = abbreviate(systemPrompt.replaceAll("\\s+", " ").trim(), 60);
            }

            GeneratedCloneProfile profile = new GeneratedCloneProfile(alias, shortDescription, systemPrompt);
            PromptGenerationLog log = saveGenerationLog(user, request.answers(), profile);
            return new PromptGenerateResult(log.getId(), log.getAlias(), log.getShortDescription(), log.getSystemPrompt());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        }
    }

    private PromptGenerationLog saveGenerationLog(
            UserAccount user,
            List<PromptGenerateRequest.AnswerItem> answers,
            GeneratedCloneProfile profile
    ) throws IOException {
        String answersJson = objectMapper.writeValueAsString(answers);
        PromptGenerationLog log = new PromptGenerationLog(
                user,
                appProperties.getOpenai().getModel(),
                answersJson,
                profile.systemPrompt(),
                profile.alias(),
                profile.shortDescription()
        );
        return promptGenerationLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<CloneSummaryResponse> listClones(Long userId, AssetListScope scope) {
        authService.getActiveUserAccount(userId);
        List<PromptGenerationLog> logs = switch (scope) {
            case MINE -> promptGenerationLogRepository.findAllByUserIdOrderByIdDesc(userId);
            case FRIEND -> listFriendClones(userId);
            case PUBLIC -> promptGenerationLogRepository.findAllByIsPublicTrueOrderByIdDesc();
        };

        return logs.stream()
                .map(log -> new CloneSummaryResponse(
                        log.getId(),
                        log.getCreatedAt(),
                        log.getAlias(),
                        log.getShortDescription(),
                        log.isPublic(),
                        log.getUser() != null ? log.getUser().getDisplayName() : null
                ))
                .toList();
    }

    private List<PromptGenerationLog> listFriendClones(Long userId) {
        List<Long> friendIds = friendRelationshipService.findAcceptedFriendIds(userId);
        if (friendIds.isEmpty()) {
            return List.of();
        }
        return promptGenerationLogRepository.findAllByUserIdInAndIsPublicFalseOrderByIdDesc(friendIds);
    }

    public CloneVisibilityResponse updateCloneVisibility(Long userId, Long cloneId, VisibilityUpdateRequest request) {
        if (request == null || request.isPublic() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isPublic is required.");
        }

        PromptGenerationLog clone = cloneAccessPolicy.getManageableClone(userId, cloneId);
        clone.updateVisibility(request.isPublic());
        PromptGenerationLog saved = promptGenerationLogRepository.save(clone);
        return new CloneVisibilityResponse(saved.getId(), saved.isPublic());
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

    private String normalizeSystemPrompt(String systemPrompt) {
        return systemPrompt == null ? "" : systemPrompt.trim();
    }

    private String normalizeAlias(String alias) {
        String normalized = Normalizer.normalize(alias == null ? "" : alias, Normalizer.Form.NFC).trim();
        normalized = normalized.replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private String normalizeShortDescription(String shortDescription) {
        String normalized = (shortDescription == null ? "" : shortDescription).replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private record GeneratedCloneProfile(String alias, String shortDescription, String systemPrompt) {
    }
}
