package com.ssarvis.backend.voice;

import com.ssarvis.backend.friend.FriendRelationshipService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoiceAccessPolicy {

    private final RegisteredVoiceRepository registeredVoiceRepository;
    private final FriendRelationshipService friendRelationshipService;

    public VoiceAccessPolicy(
            RegisteredVoiceRepository registeredVoiceRepository,
            FriendRelationshipService friendRelationshipService
    ) {
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.friendRelationshipService = friendRelationshipService;
    }

    public RegisteredVoice getManageableVoice(Long userId, Long registeredVoiceId) {
        return registeredVoiceRepository.findByIdAndUserId(registeredVoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found."));
    }

    public RegisteredVoice getReadableVoice(Long userId, Long registeredVoiceId) {
        RegisteredVoice voice = registeredVoiceRepository.findById(registeredVoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found."));
        if (isReadableByUser(userId, voice)) {
            return voice;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered voice not found.");
    }

    public RegisteredVoice getUsableVoice(Long userId, Long registeredVoiceId) {
        return getReadableVoice(userId, registeredVoiceId);
    }

    private boolean isReadableByUser(Long userId, RegisteredVoice voice) {
        if (voice.getUser() == null || voice.getUser().isDeleted()) {
            return false;
        }
        if (voice.getUser().getId().equals(userId)) {
            return true;
        }
        if (voice.isPublic()) {
            return true;
        }
        return friendRelationshipService.areFriends(userId, voice.getUser().getId());
    }
}
