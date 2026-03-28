package com.ssarvis.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountVisibility visibility = AccountVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AutoReplyMode autoReplyMode = AutoReplyMode.OFF;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant lastActivityAt;

    @Column
    private Instant deletedAt;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        lastActivityAt = createdAt;
    }

    public void softDelete() {
        deletedAt = Instant.now();
    }

    public void updateVisibility(AccountVisibility visibility) {
        this.visibility = visibility == null ? AccountVisibility.PUBLIC : visibility;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updateAutoReplyMode(AutoReplyMode autoReplyMode) {
        this.autoReplyMode = autoReplyMode == null ? AutoReplyMode.OFF : autoReplyMode;
    }

    public void touchActivity() {
        this.lastActivityAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AccountVisibility getVisibility() {
        return visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AutoReplyMode getAutoReplyMode() {
        return autoReplyMode;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPrivateAccount() {
        return visibility == AccountVisibility.PRIVATE;
    }
}
