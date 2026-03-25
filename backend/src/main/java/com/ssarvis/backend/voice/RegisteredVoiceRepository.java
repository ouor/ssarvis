package com.ssarvis.backend.voice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisteredVoiceRepository extends JpaRepository<RegisteredVoice, Long> {
    List<RegisteredVoice> findAllByOrderByIdDesc();
}
