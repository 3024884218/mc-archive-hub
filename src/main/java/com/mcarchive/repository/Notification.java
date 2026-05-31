package com.mcarchive.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知实体 — 用户操作的通知（评论、点赞等）
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收通知的用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 通知类型: comment / like */
    @Column(nullable = false, length = 20)
    private String type;

    /** 相关存档 ID */
    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    /** 触发操作的用户名 */
    @Column(length = 30)
    private String actorName;

    /** 存档标题 */
    @Column(length = 100)
    private String archiveTitle;

    /** 是否已读 */
    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(Long userId, String type, Long archiveId, String actorName, String archiveTitle) {
        this.userId = userId;
        this.type = type;
        this.archiveId = archiveId;
        this.actorName = actorName;
        this.archiveTitle = archiveTitle;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getArchiveId() { return archiveId; }
    public void setArchiveId(Long archiveId) { this.archiveId = archiveId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getArchiveTitle() { return archiveTitle; }
    public void setArchiveTitle(String archiveTitle) { this.archiveTitle = archiveTitle; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
