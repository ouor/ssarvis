package com.ssarvis.backend.debate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebateSessionRepository extends JpaRepository<DebateSession, Long> {
    Optional<DebateSession> findByIdAndUserId(Long id, Long userId);
}
