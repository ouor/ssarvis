package com.ssarvis.backend.debate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "debate_turns")
public class DebateTurn {

    public enum Speaker {
        CLONE_A,
        CLONE_B
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debate_session_id", nullable = false)
    private DebateSession debateSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Speaker speaker;

    @Column(nullable = false)
    private int turnIndex;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DebateTurn() {
    }

    public DebateTurn(DebateSession debateSession, Speaker speaker, int turnIndex, String content) {
        this.debateSession = debateSession;
        this.speaker = speaker;
        this.turnIndex = turnIndex;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public DebateSession getDebateSession() {
        return debateSession;
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public int getTurnIndex() {
        return turnIndex;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
