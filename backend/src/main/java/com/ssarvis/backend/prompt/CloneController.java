package com.ssarvis.backend.prompt;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clones")
public class CloneController {

    private final PromptGenerationLogRepository promptGenerationLogRepository;

    public CloneController(PromptGenerationLogRepository promptGenerationLogRepository) {
        this.promptGenerationLogRepository = promptGenerationLogRepository;
    }

    @GetMapping
    public List<CloneSummaryResponse> listClones() {
        return promptGenerationLogRepository.findAllByOrderByIdDesc().stream()
                .map(log -> new CloneSummaryResponse(
                        log.getId(),
                        log.getCreatedAt(),
                        log.getAlias(),
                        log.getShortDescription()
                ))
                .toList();
    }
}
