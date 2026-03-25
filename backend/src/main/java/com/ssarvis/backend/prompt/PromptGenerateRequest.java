package com.ssarvis.backend.prompt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PromptGenerateRequest(
        @NotEmpty(message = "answers must contain at least one item.")
        List<@Valid AnswerItem> answers
) {

    public record AnswerItem(
            @NotBlank(message = "question must not be blank.")
            String question,
            @NotBlank(message = "answer must not be blank.")
            String answer
    ) {
    }
}
