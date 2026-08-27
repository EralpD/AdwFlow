package com.example.demo.works;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "history")
public class HistoryWork {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String brief;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "json")
    private WorkContent content;
    @Column(name = "is_liked", nullable = false)
    private boolean liked;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected HistoryWork() {}

    public HistoryWork(Long userId, String title, String brief, WorkContent content,
            Instant createdAt, Instant expiresAt) {
        this.userId = userId;
        this.title = title;
        this.brief = brief;
        this.content = content;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getBrief() { return brief; }
    public WorkContent getContent() { return content; }
    public boolean isLiked() { return liked; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
