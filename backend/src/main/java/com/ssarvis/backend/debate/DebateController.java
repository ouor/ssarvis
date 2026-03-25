package com.ssarvis.backend.debate;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/{debateSessionId}/next")
    public DebateProgressResponse createNextTurn(@PathVariable Long debateSessionId) {
        return debateService.createNextTurn(debateSessionId);
    }

    @PostMapping("/{debateSessionId}/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stopDebate(@PathVariable Long debateSessionId) {
        debateService.stopDebate(debateSessionId);
    }
}
