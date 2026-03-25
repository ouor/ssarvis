package com.ssarvis.backend.chat;

import com.ssarvis.backend.voice.GeneratedAudioAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    public enum Role {
        USER,
        ASSISTANT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_asset_id")
    private GeneratedAudioAsset audioAsset;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(ChatConversation conversation, Role role, String content) {
        this(conversation, role, content, null);
    }

    public ChatMessage(ChatConversation conversation, Role role, String content, GeneratedAudioAsset audioAsset) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.audioAsset = audioAsset;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ChatConversation getConversation() {
        return conversation;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public GeneratedAudioAsset getAudioAsset() {
        return audioAsset;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
