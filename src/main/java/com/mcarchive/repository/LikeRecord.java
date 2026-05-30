package com.mcarchive.repository;

import com.mcarchive.model.Archive;
import com.mcarchive.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 点赞记录实体 — 存储 user 和 archive 的点赞关系
 */
@Entity
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "archive_id"}))
public class LikeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id", nullable = false)
    private Archive archive;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public LikeRecord() {}

    public LikeRecord(User user, Archive archive) {
        this.user = user;
        this.archive = archive;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Archive getArchive() { return archive; }
    public void setArchive(Archive archive) { this.archive = archive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
