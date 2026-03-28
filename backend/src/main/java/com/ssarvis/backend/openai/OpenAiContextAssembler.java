package com.ssarvis.backend.openai;

import com.ssarvis.backend.chat.ChatMessage;
import com.ssarvis.backend.dm.DmMessage;
import com.ssarvis.backend.prompt.PromptGenerateRequest;
import com.ssarvis.backend.prompt.PromptTemplates;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenAiContextAssembler {

    public List<OpenAiMessage> buildSystemPromptGenerationMessages(List<PromptGenerateRequest.AnswerItem> answers) {
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

    public List<OpenAiMessage> buildAliasGenerationMessages(String systemPrompt) {
        return List.of(
                new OpenAiMessage("system", PromptTemplates.CLONE_ALIAS_GENERATOR_SYSTEM),
                new OpenAiMessage("user", PromptTemplates.CLONE_ALIAS_GENERATOR_USER.formatted(systemPrompt))
        );
    }

    public List<OpenAiMessage> buildShortDescriptionGenerationMessages(String systemPrompt) {
        return List.of(
                new OpenAiMessage("system", PromptTemplates.CLONE_SHORT_DESCRIPTION_GENERATOR_SYSTEM),
                new OpenAiMessage("user", PromptTemplates.CLONE_SHORT_DESCRIPTION_GENERATOR_USER.formatted(systemPrompt))
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

        messages.add(new OpenAiMessage("system", PromptTemplates.CHAT_GENERATION_INSTRUCTION));
        messages.add(new OpenAiMessage("user", userMessage));
        return messages;
    }

    public List<OpenAiMessage> buildDebateMessages(
            String systemPrompt,
            String topic,
            String activeSpeakerName,
            List<DebateHistoryMessage> history,
            int maxTurns
    ) {
        List<OpenAiMessage> messages = new ArrayList<>();
        messages.add(new OpenAiMessage("system", systemPrompt));

        for (DebateHistoryMessage message : limitDebateHistoryToRecentTurns(history, maxTurns)) {
            String role = activeSpeakerName.equals(message.speakerName()) ? "user" : "assistant";
            messages.add(new OpenAiMessage(role, message.content()));
        }

        messages.add(new OpenAiMessage("system", PromptTemplates.DEBATE_GENERATION_INSTRUCTION.formatted(topic)));
        return messages;
    }

    public List<OpenAiMessage> buildDmAutoReplyMessages(
            Long accountOwnerUserId,
            String systemPrompt,
            List<DmMessage> history,
            int maxTurns
    ) {
        List<OpenAiMessage> messages = new ArrayList<>();
        messages.add(new OpenAiMessage("system", systemPrompt));

        for (DmMessage message : limitDmHistoryToRecentTurns(history, maxTurns)) {
            String role = message.getSender().getId().equals(accountOwnerUserId) ? "assistant" : "user";
            messages.add(new OpenAiMessage(role, message.getContent()));
        }

        messages.add(new OpenAiMessage("system", PromptTemplates.DM_AUTO_REPLY_GENERATION_INSTRUCTION));
        return messages;
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

    private List<DebateHistoryMessage> limitDebateHistoryToRecentTurns(List<DebateHistoryMessage> history, int maxTurns) {
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

    private List<DmMessage> limitDmHistoryToRecentTurns(List<DmMessage> history, int maxTurns) {
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

    public record DebateHistoryMessage(String speakerName, String content) {
    }
}
