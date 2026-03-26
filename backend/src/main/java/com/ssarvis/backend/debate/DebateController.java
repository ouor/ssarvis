package com.ssarvis.backend.debate;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/debates")
public class DebateController {

    private final DebateService debateService;

    public DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

    @GetMapping
    public List<DebateSessionSummaryResponse> listDebates(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return debateService.listDebates(user.userId());
    }

    @GetMapping("/{debateSessionId}")
    public DebateSessionDetailResponse getDebate(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long debateSessionId
    ) {
        return debateService.getDebate(user.userId(), debateSessionId);
    }

    @PostMapping
    public DebateProgressResponse startDebate(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @Valid @RequestBody DebateStartRequest request
    ) {
        return debateService.startDebate(user.userId(), request);
    }

    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public StreamingResponseBody startDebateStream(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @Valid @RequestBody DebateStartRequest request
    ) {
        return outputStream -> debateService.streamStartDebate(user.userId(), request, outputStream);
    }

    @PostMapping("/{debateSessionId}/next")
    public DebateProgressResponse createNextTurn(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long debateSessionId
    ) {
        return debateService.createNextTurn(user.userId(), debateSessionId);
    }

    @PostMapping(value = "/{debateSessionId}/next/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public StreamingResponseBody createNextTurnStream(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long debateSessionId
    ) {
        return outputStream -> debateService.streamNextTurn(user.userId(), debateSessionId, outputStream);
    }
}
