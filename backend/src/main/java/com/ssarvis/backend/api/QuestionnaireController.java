package com.ssarvis.backend.api;

import com.ssarvis.backend.prompt.PromptGenerateRequest;
import com.ssarvis.backend.prompt.PromptGenerateResult;
import com.ssarvis.backend.prompt.PromptGenerateResponse;
import com.ssarvis.backend.prompt.PromptService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QuestionnaireController {

    private final PromptService promptService;

    public QuestionnaireController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping("/system-prompt")
    public PromptGenerateResponse generatePrompt(@Valid @RequestBody PromptGenerateRequest request) {
        PromptGenerateResult result = promptService.generateSystemPrompt(request);
        return new PromptGenerateResponse(
                result.promptGenerationLogId(),
                result.alias(),
                result.shortDescription(),
                result.systemPrompt()
        );
    }
}
