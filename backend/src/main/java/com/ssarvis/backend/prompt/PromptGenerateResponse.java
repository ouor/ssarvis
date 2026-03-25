package com.ssarvis.backend.prompt;

public record PromptGenerateResponse(
        Long promptGenerationLogId,
        String alias,
        String shortDescription,
        String systemPrompt
) {
}
