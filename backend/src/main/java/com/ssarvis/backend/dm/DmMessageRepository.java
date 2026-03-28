package com.ssarvis.backend.dm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {

    List<DmMessage> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    List<DmMessage> findByThreadIdOrderByCreatedAtDesc(Long threadId);
}
