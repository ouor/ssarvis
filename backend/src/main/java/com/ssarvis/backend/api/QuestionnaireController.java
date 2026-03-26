package com.ssarvis.backend.api;

import com.ssarvis.backend.prompt.PromptGenerateRequest;
import com.ssarvis.backend.prompt.PromptGenerateResult;
import com.ssarvis.backend.prompt.PromptGenerateResponse;
import com.ssarvis.backend.prompt.PromptService;
import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
    public PromptGenerateResponse generatePrompt(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @Valid @RequestBody PromptGenerateRequest request
    ) {
        PromptGenerateResult result = promptService.generateSystemPrompt(user.userId(), request);
        return new PromptGenerateResponse(
                result.promptGenerationLogId(),
                result.alias(),
                result.shortDescription(),
                result.systemPrompt()
        );
    }
}
