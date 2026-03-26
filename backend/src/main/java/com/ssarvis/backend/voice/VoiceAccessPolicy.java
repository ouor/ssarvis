package com.ssarvis.backend.voice;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoiceAccessPolicy {

    private final RegisteredVoiceRepository registeredVoiceRepository;

    public VoiceAccessPolicy(RegisteredVoiceRepository registeredVoiceRepository) {
        this.registeredVoiceRepository = registeredVoiceRepository;
    }

    public RegisteredVoice getManageableVoice(Long userId, Long registeredVoiceId) {
        return registeredVoiceRepository.findByIdAndUserId(registeredVoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found."));
    }

    public RegisteredVoice getReadableVoice(Long userId, Long registeredVoiceId) {
        return registeredVoiceRepository.findReadableById(registeredVoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found."));
    }

    public RegisteredVoice getUsableVoice(Long userId, Long registeredVoiceId) {
        return getReadableVoice(userId, registeredVoiceId);
    }
}
