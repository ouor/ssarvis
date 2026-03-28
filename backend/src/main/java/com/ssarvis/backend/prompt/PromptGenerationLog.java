package com.ssarvis.backend.prompt;

import com.ssarvis.backend.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "prompt_generation_logs")
public class PromptGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 100)
    private String model;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String answersJson;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String systemPrompt;

    @Column(nullable = false, length = 120)
    private String alias;

    @Column(nullable = false, length = 255)
    private String shortDescription;

    @Column(nullable = false)
    private boolean isPublic;

    protected PromptGenerationLog() {
    }

    public PromptGenerationLog(String model, String answersJson, String systemPrompt, String alias, String shortDescription) {
        this(null, model, answersJson, systemPrompt, alias, shortDescription);
    }

    public PromptGenerationLog(
            UserAccount user,
            String model,
            String answersJson,
            String systemPrompt,
            String alias,
            String shortDescription
    ) {
        this.user = user;
        this.model = model;
        this.answersJson = answersJson;
        this.systemPrompt = systemPrompt;
        this.alias = alias;
        this.shortDescription = shortDescription;
        this.isPublic = false;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getModel() {
        return model;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getAlias() {
        return alias;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void updateProfile(String model, String answersJson, String systemPrompt, String alias, String shortDescription) {
        this.model = model;
        this.answersJson = answersJson;
        this.systemPrompt = systemPrompt;
        this.alias = alias;
        this.shortDescription = shortDescription;
        this.isPublic = false;
    }

    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
