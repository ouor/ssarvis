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
                        summarize(log.getSystemPrompt())
                ))
                .toList();
    }

    private String summarize(String systemPrompt) {
        if (systemPrompt == null) {
            return "";
        }
        String normalized = systemPrompt.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140) + "...";
    }
}
