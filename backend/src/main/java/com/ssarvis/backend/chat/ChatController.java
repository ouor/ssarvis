package com.ssarvis.backend.chat;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/messages")
    public ChatResponse sendMessage(@Valid @RequestBody ChatRequest request) {
        ChatResult result = chatService.reply(request);
        return new ChatResponse(
                result.conversationId(),
                result.assistantMessage(),
                result.ttsVoiceId(),
                result.ttsAudioMimeType(),
                result.ttsAudioBase64()
        );
    }
}
