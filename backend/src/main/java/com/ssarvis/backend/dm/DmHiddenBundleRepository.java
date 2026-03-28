package com.ssarvis.backend.dm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmHiddenBundleRepository extends JpaRepository<DmHiddenBundle, Long> {

    List<DmHiddenBundle> findAllByViewerIdAndThreadIdOrderByIdAsc(Long viewerUserId, Long threadId);

    boolean existsByViewerIdAndBundleRootMessageId(Long viewerUserId, Long bundleRootMessageId);

    void deleteByViewerIdAndBundleRootMessageId(Long viewerUserId, Long bundleRootMessageId);
}
