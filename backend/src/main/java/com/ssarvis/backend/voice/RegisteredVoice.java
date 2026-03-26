package com.ssarvis.backend.voice;

import com.ssarvis.backend.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "registered_voices")
public class RegisteredVoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false, length = 200)
    private String providerVoiceId;

    @Column(nullable = false, length = 100)
    private String targetModel;

    @Column(nullable = false, length = 255)
    private String preferredName;

    @Column(length = 255)
    private String displayName;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String audioMimeType;

    @Column(nullable = false)
    private boolean isPublic;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RegisteredVoice() {
    }

    public RegisteredVoice(
            String providerVoiceId,
            String targetModel,
            String preferredName,
            String displayName,
            String originalFilename,
            String audioMimeType
    ) {
        this(null, providerVoiceId, targetModel, preferredName, displayName, originalFilename, audioMimeType);
    }

    public RegisteredVoice(
            UserAccount user,
            String providerVoiceId,
            String targetModel,
            String preferredName,
            String displayName,
            String originalFilename,
            String audioMimeType
    ) {
        this.user = user;
        this.providerVoiceId = providerVoiceId;
        this.targetModel = targetModel;
        this.preferredName = preferredName;
        this.displayName = displayName;
        this.originalFilename = originalFilename;
        this.audioMimeType = audioMimeType;
        this.isPublic = false;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
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

    public String getDisplayName() {
        return (displayName == null || displayName.isBlank()) ? preferredName : displayName;
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

    public boolean isPublic() {
        return isPublic;
    }

    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
