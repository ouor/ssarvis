package com.ssarvis.backend.debate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebateTurnRepository extends JpaRepository<DebateTurn, Long> {
    List<DebateTurn> findByDebateSessionIdOrderByTurnIndexAsc(Long debateSessionId);
}
