package com.ssarvis.backend.openai;

import java.util.List;

public record OpenAiChatCompletionRequest(
        String model,
        List<OpenAiMessage> messages
) {
}
