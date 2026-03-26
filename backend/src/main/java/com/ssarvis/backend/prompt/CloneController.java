package com.ssarvis.backend.prompt;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
    public List<CloneSummaryResponse> listClones(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return promptGenerationLogRepository.findAllByUserIdOrderByIdDesc(user.userId()).stream()
                .map(log -> new CloneSummaryResponse(
                        log.getId(),
                        log.getCreatedAt(),
                        log.getAlias(),
                        log.getShortDescription()
                ))
                .toList();
    }
}
