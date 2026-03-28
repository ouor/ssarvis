package com.ssarvis.backend.dm;

import com.ssarvis.backend.auth.UserAccount;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "dm_threads",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dm_threads_participants", columnNames = {"participant_a_user_id", "participant_b_user_id"})
        }
)
public class DmThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_a_user_id", nullable = false)
    private UserAccount participantA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_b_user_id", nullable = false)
    private UserAccount participantB;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DmThread() {
    }

    public DmThread(UserAccount participantA, UserAccount participantB) {
        this.participantA = participantA;
        this.participantB = participantB;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getParticipantA() {
        return participantA;
    }

    public UserAccount getParticipantB() {
        return participantB;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean involves(Long userId) {
        return participantA.getId().equals(userId) || participantB.getId().equals(userId);
    }

    public UserAccount otherParticipant(Long userId) {
        return participantA.getId().equals(userId) ? participantB : participantA;
    }
}
