package com.ssarvis.backend.debate;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/debates")
public class DebateController {

    private final DebateService debateService;

    public DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

    @PostMapping
    public DebateProgressResponse startDebate(@Valid @RequestBody DebateStartRequest request) {
        return debateService.startDebate(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public StreamingResponseBody startDebateStream(@Valid @RequestBody DebateStartRequest request) {
        return outputStream -> debateService.streamStartDebate(request, outputStream);
    }

    @PostMapping("/{debateSessionId}/next")
    public DebateProgressResponse createNextTurn(@PathVariable Long debateSessionId) {
        return debateService.createNextTurn(debateSessionId);
    }

    @PostMapping(value = "/{debateSessionId}/next/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public StreamingResponseBody createNextTurnStream(@PathVariable Long debateSessionId) {
        return outputStream -> debateService.streamNextTurn(debateSessionId, outputStream);
    }

    @PostMapping("/{debateSessionId}/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stopDebate(@PathVariable Long debateSessionId) {
        debateService.stopDebate(debateSessionId);
    }
}
