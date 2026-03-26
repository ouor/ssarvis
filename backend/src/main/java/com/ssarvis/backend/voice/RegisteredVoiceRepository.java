package com.ssarvis.backend.voice;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisteredVoiceRepository extends JpaRepository<RegisteredVoice, Long> {
    List<RegisteredVoice> findAllByOrderByIdDesc();

    List<RegisteredVoice> findAllByUserIdOrderByIdDesc(Long userId);

    Optional<RegisteredVoice> findByIdAndUserId(Long id, Long userId);
}
