package com.ssarvis.backend.prompt;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptGenerationLogRepository extends JpaRepository<PromptGenerationLog, Long> {
    List<PromptGenerationLog> findAllByOrderByIdDesc();

    List<PromptGenerationLog> findAllByUserIdOrderByIdDesc(Long userId);

    List<PromptGenerationLog> findAllByIsPublicTrueOrderByIdDesc();

    List<PromptGenerationLog> findAllByUserIdInAndIsPublicFalseOrderByIdDesc(List<Long> userIds);

    Optional<PromptGenerationLog> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select log
            from PromptGenerationLog log
            where log.id = :id
              and (log.user.id = :userId or log.isPublic = true)
            """)
    Optional<PromptGenerationLog> findReadableById(@Param("id") Long id, @Param("userId") Long userId);
}
