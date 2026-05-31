package com.mcarchive.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 下载记录 — 记录用户的下载历史
 */
@Entity
@Table(name = "download_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "archive_id"})
})
public class DownloadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(nullable = false)
    private LocalDateTime downloadedAt;

    public DownloadRecord() {}

    public DownloadRecord(Long userId, Long archiveId) {
        this.userId = userId;
        this.archiveId = archiveId;
        this.downloadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getArchiveId() { return archiveId; }
    public void setArchiveId(Long archiveId) { this.archiveId = archiveId; }
    public LocalDateTime getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(LocalDateTime downloadedAt) { this.downloadedAt = downloadedAt; }
}
