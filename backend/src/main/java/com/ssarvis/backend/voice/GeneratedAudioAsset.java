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
@Table(name = "generated_audio_assets")
public class GeneratedAudioAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false, length = 20)
    private String storageProvider;

    @Column(nullable = false, length = 100)
    private String bucketName;

    @Column(nullable = false, length = 500)
    private String objectKey;

    @Column(nullable = false, length = 1000)
    private String objectUrl;

    @Column(nullable = false, length = 100)
    private String sourceAudioMimeType;

    @Column(nullable = false, length = 100)
    private String storedAudioMimeType;

    @Column(nullable = false)
    private long sourceAudioBytes;

    @Column(nullable = false)
    private long storedAudioBytes;

    @Column(nullable = false, length = 200)
    private String providerVoiceId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected GeneratedAudioAsset() {
    }

    public GeneratedAudioAsset(
            String storageProvider,
            String bucketName,
            String objectKey,
            String objectUrl,
            String sourceAudioMimeType,
            String storedAudioMimeType,
            long sourceAudioBytes,
            long storedAudioBytes,
            String providerVoiceId
    ) {
        this(null, storageProvider, bucketName, objectKey, objectUrl, sourceAudioMimeType, storedAudioMimeType, sourceAudioBytes, storedAudioBytes, providerVoiceId);
    }

    public GeneratedAudioAsset(
            UserAccount user,
            String storageProvider,
            String bucketName,
            String objectKey,
            String objectUrl,
            String sourceAudioMimeType,
            String storedAudioMimeType,
            long sourceAudioBytes,
            long storedAudioBytes,
            String providerVoiceId
    ) {
        this.user = user;
        this.storageProvider = storageProvider;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.objectUrl = objectUrl;
        this.sourceAudioMimeType = sourceAudioMimeType;
        this.storedAudioMimeType = storedAudioMimeType;
        this.sourceAudioBytes = sourceAudioBytes;
        this.storedAudioBytes = storedAudioBytes;
        this.providerVoiceId = providerVoiceId;
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

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getObjectUrl() {
        return objectUrl;
    }

    public String getSourceAudioMimeType() {
        return sourceAudioMimeType;
    }

    public String getStoredAudioMimeType() {
        return storedAudioMimeType;
    }

    public long getSourceAudioBytes() {
        return sourceAudioBytes;
    }

    public long getStoredAudioBytes() {
        return storedAudioBytes;
    }

    public String getProviderVoiceId() {
        return providerVoiceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
