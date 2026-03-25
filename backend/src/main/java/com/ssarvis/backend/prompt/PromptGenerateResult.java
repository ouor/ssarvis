package com.ssarvis.backend.prompt;

public record PromptGenerateResult(
        Long promptGenerationLogId,
        String alias,
        String shortDescription,
        String systemPrompt
) {
}
