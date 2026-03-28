package com.ssarvis.backend.dm;

import com.ssarvis.backend.auth.AccountVisibility;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.AutoReplyMode;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.follow.FollowRepository;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DmService {
    private static final Duration AWAY_THRESHOLD = Duration.ofMinutes(3);

    private final DmThreadRepository dmThreadRepository;
    private final DmMessageRepository dmMessageRepository;
    private final AuthService authService;
    private final FollowRepository followRepository;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final OpenAiContextAssembler openAiContextAssembler;
    private final OpenAiClient openAiClient;
    private final AppProperties appProperties;

    public DmService(
            DmThreadRepository dmThreadRepository,
            DmMessageRepository dmMessageRepository,
            AuthService authService,
            FollowRepository followRepository,
            PromptGenerationLogRepository promptGenerationLogRepository,
            OpenAiContextAssembler openAiContextAssembler,
            OpenAiClient openAiClient,
            AppProperties appProperties
    ) {
        this.dmThreadRepository = dmThreadRepository;
        this.dmMessageRepository = dmMessageRepository;
        this.authService = authService;
        this.followRepository = followRepository;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.openAiContextAssembler = openAiContextAssembler;
        this.openAiClient = openAiClient;
        this.appProperties = appProperties;
    }

    @Transactional
    public DmThreadDetailResponse startThread(Long currentUserId, DmStartRequest request) {
        if (request == null || request.targetUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetUserId is required.");
        }
        if (currentUserId.equals(request.targetUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot start a DM with yourself.");
        }

        UserAccount currentUser = authService.getActiveUserAccount(currentUserId);
        UserAccount targetUser = authService.getActiveUserAccount(request.targetUserId());
        validateDmStartAllowed(currentUserId, targetUser);

        return dmThreadRepository.findByParticipants(currentUserId, targetUser.getId())
                .map(thread -> toDetail(currentUserId, thread, dmMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId())))
                .orElseGet(() -> {
                    DmThread thread = dmThreadRepository.save(new DmThread(firstParticipant(currentUser, targetUser), secondParticipant(currentUser, targetUser)));
                    return toDetail(currentUserId, thread, List.of());
                });
    }

    @Transactional(readOnly = true)
    public List<DmThreadSummaryResponse> listThreads(Long currentUserId) {
        authService.getActiveUserAccount(currentUserId);
        return dmThreadRepository.findAllByParticipantIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(thread -> toSummary(currentUserId, thread, latestMessage(thread.getId())))
                .sorted(Comparator.comparing(DmThreadSummaryResponse::latestMessageCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(DmThreadSummaryResponse::createdAt, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DmThreadDetailResponse getThread(Long currentUserId, Long threadId) {
        authService.getActiveUserAccount(currentUserId);
        DmThread thread = getAccessibleThread(currentUserId, threadId);
        return toDetail(currentUserId, thread, dmMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId));
    }

    @Transactional
    public DmMessageResponse sendMessage(Long currentUserId, Long threadId, DmSendMessageRequest request) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank.");
        }

        UserAccount sender = authService.getActiveUserAccount(currentUserId);
        DmThread thread = getAccessibleThread(currentUserId, threadId);
        DmMessage message = dmMessageRepository.save(new DmMessage(thread, sender, request.content().trim()));
        maybeCreateAutoReply(thread, sender);
        return toMessage(message);
    }

    private void validateDmStartAllowed(Long currentUserId, UserAccount targetUser) {
        if (targetUser.getVisibility() == AccountVisibility.PUBLIC) {
            return;
        }
        if (followRepository.existsByFollowerIdAndFolloweeId(currentUserId, targetUser.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This private account is not available for DM.");
    }

    private DmThread getAccessibleThread(Long currentUserId, Long threadId) {
        DmThread thread = dmThreadRepository.findWithParticipantsById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DM thread not found."));
        if (!thread.involves(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this DM thread.");
        }
        return thread;
    }

    private DmMessage latestMessage(Long threadId) {
        List<DmMessage> messages = dmMessageRepository.findByThreadIdOrderByCreatedAtDesc(threadId);
        return messages.isEmpty() ? null : messages.get(0);
    }

    private DmThreadSummaryResponse toSummary(Long currentUserId, DmThread thread, DmMessage latestMessage) {
        return new DmThreadSummaryResponse(
                thread.getId(),
                toParticipant(thread.otherParticipant(currentUserId)),
                thread.getCreatedAt(),
                latestMessage != null ? abbreviate(latestMessage.getContent(), 80) : "",
                latestMessage != null ? latestMessage.getCreatedAt() : null
        );
    }

    private DmThreadDetailResponse toDetail(Long currentUserId, DmThread thread, List<DmMessage> messages) {
        return new DmThreadDetailResponse(
                thread.getId(),
                toParticipant(thread.otherParticipant(currentUserId)),
                thread.getCreatedAt(),
                messages.stream().map(this::toMessage).toList()
        );
    }

    private DmMessageResponse toMessage(DmMessage message) {
        return new DmMessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getDisplayName(),
                message.getKind() == DmMessageKind.AI_PROXY,
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private void maybeCreateAutoReply(DmThread thread, UserAccount sender) {
        UserAccount recipient = thread.otherParticipant(sender.getId());
        if (!shouldAutoReply(recipient)) {
            return;
        }

        PromptGenerationLog clone = promptGenerationLogRepository.findTopByUserIdOrderByIdDesc(recipient.getId()).orElse(null);
        if (clone == null || !StringUtils.hasText(clone.getSystemPrompt())) {
            return;
        }

        List<DmMessage> history = dmMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
        String reply = openAiClient.requestChatCompletion(
                openAiContextAssembler.buildDmAutoReplyMessages(
                        recipient.getId(),
                        clone.getSystemPrompt(),
                        history,
                        appProperties.getOpenai().getChatHistoryTurns()
                )
        );
        if (!StringUtils.hasText(reply)) {
            return;
        }

        dmMessageRepository.save(new DmMessage(thread, recipient, reply.trim(), DmMessageKind.AI_PROXY));
    }

    private boolean shouldAutoReply(UserAccount recipient) {
        AutoReplyMode mode = recipient.getAutoReplyMode();
        if (mode == null || mode == AutoReplyMode.OFF) {
            return false;
        }
        if (mode == AutoReplyMode.ALWAYS) {
            return true;
        }

        Instant lastActivityAt = recipient.getLastActivityAt();
        if (lastActivityAt == null) {
            return true;
        }

        return lastActivityAt.isBefore(Instant.now().minus(AWAY_THRESHOLD));
    }

    private DmParticipantResponse toParticipant(UserAccount user) {
        return new DmParticipantResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getVisibility());
    }

    private UserAccount firstParticipant(UserAccount first, UserAccount second) {
        return first.getId() < second.getId() ? first : second;
    }

    private UserAccount secondParticipant(UserAccount first, UserAccount second) {
        return first.getId() < second.getId() ? second : first;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
