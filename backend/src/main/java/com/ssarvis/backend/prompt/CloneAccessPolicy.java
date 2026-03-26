package com.ssarvis.backend.prompt;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CloneAccessPolicy {

    private final PromptGenerationLogRepository promptGenerationLogRepository;

    public CloneAccessPolicy(PromptGenerationLogRepository promptGenerationLogRepository) {
        this.promptGenerationLogRepository = promptGenerationLogRepository;
    }

    public PromptGenerationLog getManageableClone(Long userId, Long cloneId) {
        return promptGenerationLogRepository.findByIdAndUserId(cloneId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt generation log not found."));
    }

    public PromptGenerationLog getReadableClone(Long userId, Long cloneId) {
        return promptGenerationLogRepository.findReadableById(cloneId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt generation log not found."));
    }

    public PromptGenerationLog getUsableClone(Long userId, Long cloneId) {
        return getReadableClone(userId, cloneId);
    }
}
