package com.ssarvis.backend.dm;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DmThreadRepository extends JpaRepository<DmThread, Long> {

    @Query("""
            select thread
            from DmThread thread
            join fetch thread.participantA participantA
            join fetch thread.participantB participantB
            where (
                    participantA.id = :firstUserId and participantB.id = :secondUserId
                  ) or (
                    participantA.id = :secondUserId and participantB.id = :firstUserId
                  )
            """)
    Optional<DmThread> findByParticipants(@Param("firstUserId") Long firstUserId, @Param("secondUserId") Long secondUserId);

    @Query("""
            select thread
            from DmThread thread
            join fetch thread.participantA participantA
            join fetch thread.participantB participantB
            where participantA.id = :userId or participantB.id = :userId
            order by thread.createdAt desc
            """)
    List<DmThread> findAllByParticipantIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
            select thread
            from DmThread thread
            join fetch thread.participantA participantA
            join fetch thread.participantB participantB
            where thread.id = :threadId
            """)
    Optional<DmThread> findWithParticipantsById(@Param("threadId") Long threadId);
}
