package com.ssarvis.backend.chat;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/messages")
    public ChatResponse sendMessage(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResult result = chatService.reply(user.userId(), request);
        return new ChatResponse(
                result.conversationId(),
                result.assistantMessage(),
                result.ttsVoiceId(),
                result.ttsAudioMimeType(),
                result.ttsAudioBase64()
        );
    }

    @PostMapping(value = "/messages/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public StreamingResponseBody streamMessage(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @Valid @RequestBody ChatRequest request
    ) {
        return outputStream -> chatService.streamReply(user.userId(), request, outputStream);
    }
}
