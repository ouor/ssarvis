package com.ssarvis.backend.prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "prompt_generation_logs")
public class PromptGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    protected PromptGenerationLog() {
    }

    public PromptGenerationLog(String model, String answersJson, String systemPrompt) {
        this.model = model;
        this.answersJson = answersJson;
        this.systemPrompt = systemPrompt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
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
}
