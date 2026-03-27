package com.ssarvis.backend.friend;

import com.ssarvis.backend.auth.UserAccount;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "friend_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_friend_request_pending_pair", columnNames = "pending_pair_key")
        }
)
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserAccount requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserAccount receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant respondedAt;

    @Column(name = "pending_pair_key", length = 64, unique = true)
    private String pendingPairKey;

    protected FriendRequest() {
    }

    public FriendRequest(UserAccount requester, UserAccount receiver) {
        this.requester = Objects.requireNonNull(requester, "requester must not be null");
        this.receiver = Objects.requireNonNull(receiver, "receiver must not be null");
        validateDistinctUsers();
        this.status = FriendRequestStatus.PENDING;
        synchronizePendingPairKey();
    }

    @PrePersist
    void onCreate() {
        validateDistinctUsers();
        createdAt = Instant.now();
        synchronizePendingPairKey();
    }

    @PreUpdate
    void onUpdate() {
        validateDistinctUsers();
        synchronizePendingPairKey();
    }

    public void accept() {
        transitionTo(FriendRequestStatus.ACCEPTED);
    }

    public void reject() {
        transitionTo(FriendRequestStatus.REJECTED);
    }

    public void cancel() {
        transitionTo(FriendRequestStatus.CANCELED);
    }

    public void unfriend() {
        transitionTo(FriendRequestStatus.CANCELED);
    }

    public boolean isPending() {
        return status == FriendRequestStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getRequester() {
        return requester;
    }

    public UserAccount getReceiver() {
        return receiver;
    }

    public FriendRequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    void transitionTo(FriendRequestStatus nextStatus) {
        this.status = Objects.requireNonNull(nextStatus, "nextStatus must not be null");
        this.respondedAt = nextStatus == FriendRequestStatus.PENDING ? null : Instant.now();
        synchronizePendingPairKey();
    }

    private void validateDistinctUsers() {
        if (requester == null || receiver == null) {
            return;
        }
        Long requesterId = requester.getId();
        Long receiverId = receiver.getId();
        if (requester == receiver
                || (requesterId != null && receiverId != null && requesterId.equals(receiverId))) {
            throw new IllegalArgumentException("Cannot create a friend request to the same user.");
        }
    }

    private void synchronizePendingPairKey() {
        if (status != FriendRequestStatus.PENDING) {
            pendingPairKey = null;
            return;
        }
        Long requesterId = requester != null ? requester.getId() : null;
        Long receiverId = receiver != null ? receiver.getId() : null;
        if (requesterId == null || receiverId == null) {
            pendingPairKey = null;
            return;
        }
        long lowerId = Math.min(requesterId, receiverId);
        long higherId = Math.max(requesterId, receiverId);
        pendingPairKey = lowerId + ":" + higherId;
    }
}
