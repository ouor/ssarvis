package com.ssarvis.backend.voice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    public java.util.List<VoiceSummaryResponse> listVoices() {
        return voiceService.listVoices();
    }

    @PostMapping(consumes = "multipart/form-data")
    public VoiceRegisterResponse registerVoice(
            @RequestParam("sample") MultipartFile sample,
            @RequestParam(value = "alias", required = false) String alias
    ) {
        RegisteredVoice voice = voiceService.registerVoice(sample, alias);
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
