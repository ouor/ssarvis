package com.ssarvis.backend.prompt;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptGenerationLogRepository extends JpaRepository<PromptGenerationLog, Long> {
    List<PromptGenerationLog> findAllByOrderByIdDesc();
}
