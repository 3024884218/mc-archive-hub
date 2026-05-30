package com.mcarchive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 评论实体
 * 用户对存档的评论
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 评论内容，最多 2000 字 */
    @Column(nullable = false, length = 2000)
    private String content;

    /** 评论者 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** 所属存档 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id", nullable = false)
    private Archive archive;

    /** 发布时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ===== Getter / Setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public Archive getArchive() { return archive; }
    public void setArchive(Archive archive) { this.archive = archive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
