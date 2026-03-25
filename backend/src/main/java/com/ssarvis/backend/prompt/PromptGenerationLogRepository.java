package com.ssarvis.backend.prompt;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptGenerationLogRepository extends JpaRepository<PromptGenerationLog, Long> {
}
