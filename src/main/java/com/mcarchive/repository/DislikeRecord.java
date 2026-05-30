package com.mcarchive.repository;

import jakarta.persistence.*;

@Entity
@Table(name = "dislike_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "archive_id"})
})
public class DislikeRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    public DislikeRecord() {}
    public DislikeRecord(Long userId, Long archiveId) {
        this.userId = userId; this.archiveId = archiveId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getArchiveId() { return archiveId; }
    public void setArchiveId(Long archiveId) { this.archiveId = archiveId; }
}
