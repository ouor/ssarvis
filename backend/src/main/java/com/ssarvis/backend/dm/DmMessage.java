package com.ssarvis.backend.dm;

import com.ssarvis.backend.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "dm_messages")
public class DmMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private DmThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private UserAccount sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_message_id")
    private DmMessage triggerMessage;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DmMessageKind kind;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DmMessageFormat format;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 100)
    private String audioMimeType;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String audioBase64;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DmMessage() {
    }

    public DmMessage(DmThread thread, UserAccount sender, String content) {
        this(thread, sender, content, DmMessageKind.HUMAN, null, DmMessageFormat.TEXT, null, null);
    }

    public DmMessage(DmThread thread, UserAccount sender, String content, DmMessageKind kind) {
        this(thread, sender, content, kind, null, DmMessageFormat.TEXT, null, null);
    }

    public DmMessage(DmThread thread, UserAccount sender, String content, DmMessageKind kind, DmMessage triggerMessage) {
        this(thread, sender, content, kind, triggerMessage, DmMessageFormat.TEXT, null, null);
    }

    public DmMessage(
            DmThread thread,
            UserAccount sender,
            String content,
            DmMessageKind kind,
            DmMessage triggerMessage,
            DmMessageFormat format,
            String audioMimeType,
            String audioBase64
    ) {
        this.thread = thread;
        this.sender = sender;
        this.content = content;
        this.kind = kind == null ? DmMessageKind.HUMAN : kind;
        this.triggerMessage = triggerMessage;
        this.format = format == null ? DmMessageFormat.TEXT : format;
        this.audioMimeType = audioMimeType;
        this.audioBase64 = audioBase64;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public DmThread getThread() {
        return thread;
    }

    public UserAccount getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public DmMessage getTriggerMessage() {
        return triggerMessage;
    }

    public DmMessageKind getKind() {
        return kind;
    }

    public DmMessageFormat getFormat() {
        return format;
    }

    public String getAudioMimeType() {
        return audioMimeType;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
