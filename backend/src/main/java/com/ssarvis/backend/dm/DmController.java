package com.ssarvis.backend.dm;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DmController {

    private final DmService dmService;

    public DmController(DmService dmService) {
        this.dmService = dmService;
    }

    @PostMapping("/api/dms/threads")
    public DmThreadDetailResponse startThread(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @Valid @RequestBody DmStartRequest request
    ) {
        return dmService.startThread(user.userId(), request);
    }

    @GetMapping("/api/dms/threads")
    public List<DmThreadSummaryResponse> listThreads(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return dmService.listThreads(user.userId());
    }

    @GetMapping("/api/dms/threads/{threadId}")
    public DmThreadDetailResponse getThread(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long threadId
    ) {
        return dmService.getThread(user.userId(), threadId);
    }

    @PostMapping("/api/dms/threads/{threadId}/messages")
    public DmMessageResponse sendMessage(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long threadId,
            @Valid @RequestBody DmSendMessageRequest request
    ) {
        return dmService.sendMessage(user.userId(), threadId, request);
    }
}
