package com.ssarvis.backend.voice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "registered_voices")
public class RegisteredVoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String providerVoiceId;

    @Column(nullable = false, length = 100)
    private String targetModel;

    @Column(nullable = false, length = 255)
    private String preferredName;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String audioMimeType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RegisteredVoice() {
    }

    public RegisteredVoice(
            String providerVoiceId,
            String targetModel,
            String preferredName,
            String originalFilename,
            String audioMimeType
    ) {
        this.providerVoiceId = providerVoiceId;
        this.targetModel = targetModel;
        this.preferredName = preferredName;
        this.originalFilename = originalFilename;
        this.audioMimeType = audioMimeType;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getProviderVoiceId() {
        return providerVoiceId;
    }

    public String getTargetModel() {
        return targetModel;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getAudioMimeType() {
        return audioMimeType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
