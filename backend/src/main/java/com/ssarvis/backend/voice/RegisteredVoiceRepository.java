package com.ssarvis.backend.voice;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegisteredVoiceRepository extends JpaRepository<RegisteredVoice, Long> {
    List<RegisteredVoice> findAllByOrderByIdDesc();

    List<RegisteredVoice> findAllByUserIdOrderByIdDesc(Long userId);

    List<RegisteredVoice> findAllByIsPublicTrueOrderByIdDesc();

    List<RegisteredVoice> findAllByUserIdInAndIsPublicFalseOrderByIdDesc(List<Long> userIds);

    Optional<RegisteredVoice> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select voice
            from RegisteredVoice voice
            where voice.id = :id
              and (voice.user.id = :userId or voice.isPublic = true)
            """)
    Optional<RegisteredVoice> findReadableById(@Param("id") Long id, @Param("userId") Long userId);
}
