package com.ssarvis.backend.voice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedAudioAssetRepository extends JpaRepository<GeneratedAudioAsset, Long> {
    List<GeneratedAudioAsset> findAllByUserIdOrderByIdDesc(Long userId);
}
