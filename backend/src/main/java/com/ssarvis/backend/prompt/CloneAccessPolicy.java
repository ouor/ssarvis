package com.ssarvis.backend.prompt;

import com.ssarvis.backend.friend.FriendRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CloneAccessPolicy {

    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final FriendRequestRepository friendRequestRepository;

    public CloneAccessPolicy(
            PromptGenerationLogRepository promptGenerationLogRepository,
            FriendRequestRepository friendRequestRepository
    ) {
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    public PromptGenerationLog getManageableClone(Long userId, Long cloneId) {
        return promptGenerationLogRepository.findByIdAndUserId(cloneId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt generation log not found."));
    }

    public PromptGenerationLog getReadableClone(Long userId, Long cloneId) {
        PromptGenerationLog clone = promptGenerationLogRepository.findById(cloneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt generation log not found."));
        if (isReadableByUser(userId, clone)) {
            return clone;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt generation log not found.");
    }

    public PromptGenerationLog getUsableClone(Long userId, Long cloneId) {
        return getReadableClone(userId, cloneId);
    }

    private boolean isReadableByUser(Long userId, PromptGenerationLog clone) {
        if (clone.getUser() == null || clone.getUser().isDeleted()) {
            return false;
        }
        if (clone.getUser().getId().equals(userId)) {
            return true;
        }
        if (clone.isPublic()) {
            return true;
        }
        return friendRequestRepository.existsAcceptedBetweenUsers(userId, clone.getUser().getId());
    }
}
