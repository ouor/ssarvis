package com.ssarvis.backend.debate;

import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.voice.RegisteredVoice;
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
@Table(name = "debate_sessions")
public class DebateSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clone_a_id", nullable = false)
    private PromptGenerationLog cloneA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clone_b_id", nullable = false)
    private PromptGenerationLog cloneB;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clone_a_voice_id", nullable = false)
    private RegisteredVoice cloneAVoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clone_b_voice_id", nullable = false)
    private RegisteredVoice cloneBVoice;

    @Column(nullable = false, length = 500)
    private String topic;

    @Column(nullable = false)
    private int turnsPerClone;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DebateSession() {
    }

    public DebateSession(
            PromptGenerationLog cloneA,
            PromptGenerationLog cloneB,
            RegisteredVoice cloneAVoice,
            RegisteredVoice cloneBVoice,
            String topic,
            int turnsPerClone
    ) {
        this.cloneA = cloneA;
        this.cloneB = cloneB;
        this.cloneAVoice = cloneAVoice;
        this.cloneBVoice = cloneBVoice;
        this.topic = topic;
        this.turnsPerClone = turnsPerClone;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public PromptGenerationLog getCloneA() {
        return cloneA;
    }

    public PromptGenerationLog getCloneB() {
        return cloneB;
    }

    public RegisteredVoice getCloneAVoice() {
        return cloneAVoice;
    }

    public RegisteredVoice getCloneBVoice() {
        return cloneBVoice;
    }

    public String getTopic() {
        return topic;
    }

    public int getTurnsPerClone() {
        return turnsPerClone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
