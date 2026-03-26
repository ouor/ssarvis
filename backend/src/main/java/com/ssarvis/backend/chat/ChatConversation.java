package com.ssarvis.backend.chat;

import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_generation_log_id", nullable = false)
    private PromptGenerationLog promptGenerationLog;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatConversation() {
    }

    public ChatConversation(PromptGenerationLog promptGenerationLog) {
        this(null, promptGenerationLog);
    }

    public ChatConversation(UserAccount user, PromptGenerationLog promptGenerationLog) {
        this.user = user;
        this.promptGenerationLog = promptGenerationLog;
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

    public PromptGenerationLog getPromptGenerationLog() {
        return promptGenerationLog;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
