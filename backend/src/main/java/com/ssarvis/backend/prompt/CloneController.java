package com.ssarvis.backend.prompt;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.access.AssetListScope;
import com.ssarvis.backend.access.VisibilityUpdateRequest;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clones")
public class CloneController {

    private final PromptService promptService;

    public CloneController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    public List<CloneSummaryResponse> listClones(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @RequestParam(value = "scope", required = false) String scope
    ) {
        return promptService.listClones(user.userId(), AssetListScope.from(scope));
    }

    @PatchMapping("/{cloneId}/visibility")
    public CloneVisibilityResponse updateCloneVisibility(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable Long cloneId,
            @Valid @RequestBody VisibilityUpdateRequest request
    ) {
        return promptService.updateCloneVisibility(user.userId(), cloneId, request);
    }
}
