package com.ssarvis.backend.debate;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DebateSessionRepository extends JpaRepository<DebateSession, Long> {
    Optional<DebateSession> findByIdAndUserId(Long id, Long userId);

    List<DebateSession> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select new com.ssarvis.backend.debate.DebateSessionSummaryRow(
                session.id,
                session.cloneA.id,
                session.cloneA.alias,
                session.cloneB.id,
                session.cloneB.alias,
                session.topic,
                session.createdAt,
                (
                    select count(turn)
                    from DebateTurn turn
                    where turn.debateSession.id = session.id
                )
            )
            from DebateSession session
            where session.user.id = :userId
            order by session.createdAt desc
            """)
    List<DebateSessionSummaryRow> findSummaryRowsByUserId(@Param("userId") Long userId);
}
