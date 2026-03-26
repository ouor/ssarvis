package com.ssarvis.backend.voice;

import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voices")
public class VoiceController {

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @GetMapping
    public java.util.List<VoiceSummaryResponse> listVoices(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user
    ) {
        return voiceService.listVoices(user.userId());
    }

    @PostMapping(consumes = "multipart/form-data")
    public VoiceRegisterResponse registerVoice(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @RequestParam("sample") MultipartFile sample,
            @RequestParam(value = "alias", required = false) String alias
    ) {
        RegisteredVoice voice = voiceService.registerVoice(user.userId(), sample, alias);
        return new VoiceRegisterResponse(
                voice.getId(),
                voice.getProviderVoiceId(),
                voice.getDisplayName(),
                voice.getPreferredName(),
                voice.getOriginalFilename(),
                voice.getAudioMimeType()
        );
    }
}
