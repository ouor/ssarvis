package com.ssarvis.backend.openai;

import com.ssarvis.backend.chat.ChatMessage;
import com.ssarvis.backend.prompt.PromptGenerateRequest;
import com.ssarvis.backend.prompt.PromptTemplates;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenAiContextAssembler {

    public List<OpenAiMessage> buildPromptGenerationMessages(List<PromptGenerateRequest.AnswerItem> answers) {
        StringBuilder answerSummary = new StringBuilder();
        for (PromptGenerateRequest.AnswerItem answer : answers) {
            if (answer == null || !StringUtils.hasText(answer.question()) || !StringUtils.hasText(answer.answer())) {
                continue;
            }
            answerSummary.append("- ")
                    .append(answer.question().trim())
                    .append(": ")
                    .append(answer.answer().trim())
                    .append('\n');
        }

        return List.of(
                new OpenAiMessage("system", PromptTemplates.SYSTEM_PROMPT_GENERATOR_SYSTEM),
                new OpenAiMessage("user", PromptTemplates.SYSTEM_PROMPT_GENERATOR_USER.formatted(answerSummary))
        );
    }

    public List<OpenAiMessage> buildChatMessages(
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage,
            int maxTurns
    ) {
        List<OpenAiMessage> messages = new ArrayList<>();
        messages.add(new OpenAiMessage("system", systemPrompt));

        for (ChatMessage message : limitHistoryToRecentTurns(history, maxTurns)) {
            String role = message.getRole() == ChatMessage.Role.USER ? "user" : "assistant";
            messages.add(new OpenAiMessage(role, message.getContent()));
        }

        messages.add(new OpenAiMessage("user", userMessage));
        return messages;
    }

    public List<OpenAiMessage> buildDebateMessages(
            String systemPrompt,
            String topic,
            String stance,
            List<String> transcriptLines
    ) {
        String transcript = transcriptLines.isEmpty()
                ? "(아직 발언 없음)\n"
                : String.join("\n", transcriptLines) + "\n";

        return List.of(
                new OpenAiMessage("system", systemPrompt),
                new OpenAiMessage("user", PromptTemplates.DEBATE_USER.formatted(topic, stance, transcript))
        );
    }

    private List<ChatMessage> limitHistoryToRecentTurns(List<ChatMessage> history, int maxTurns) {
        int normalizedMaxTurns = Math.max(maxTurns, 0);
        if (normalizedMaxTurns == 0 || history.isEmpty()) {
            return List.of();
        }

        int maxMessages = normalizedMaxTurns * 2;
        if (history.size() <= maxMessages) {
            return history;
        }

        return history.subList(history.size() - maxMessages, history.size());
    }
}
