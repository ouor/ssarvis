package com.ssarvis.backend.voice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisteredVoiceRepository extends JpaRepository<RegisteredVoice, Long> {
}
