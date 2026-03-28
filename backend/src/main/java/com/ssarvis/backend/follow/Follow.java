package com.ssarvis.backend.follow;

import com.ssarvis.backend.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_follows_follower_followee", columnNames = {"follower_user_id", "followee_user_id"})
        }
)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "follower_user_id", nullable = false)
    private UserAccount follower;

    @ManyToOne(optional = false)
    @JoinColumn(name = "followee_user_id", nullable = false)
    private UserAccount followee;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Follow() {
    }

    public Follow(UserAccount follower, UserAccount followee) {
        this.follower = follower;
        this.followee = followee;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getFollower() {
        return follower;
    }

    public UserAccount getFollowee() {
        return followee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
