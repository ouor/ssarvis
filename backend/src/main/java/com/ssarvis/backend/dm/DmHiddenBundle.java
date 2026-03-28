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
        name = "dm_hidden_bundles",
        uniqueConstraints = @UniqueConstraint(name = "uk_dm_hidden_bundle_viewer_root", columnNames = {"viewer_user_id", "bundle_root_message_id"})
)
public class DmHiddenBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewer_user_id", nullable = false)
    private UserAccount viewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private DmThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bundle_root_message_id", nullable = false)
    private DmMessage bundleRootMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DmHiddenBundle() {
    }

    public DmHiddenBundle(UserAccount viewer, DmThread thread, DmMessage bundleRootMessage) {
        this.viewer = viewer;
        this.thread = thread;
        this.bundleRootMessage = bundleRootMessage;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getViewer() {
        return viewer;
    }

    public DmThread getThread() {
        return thread;
    }

    public DmMessage getBundleRootMessage() {
        return bundleRootMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
