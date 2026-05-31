package com.mcarchive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 存档实体
 * 存储 Minecraft 存档的基本信息
 */
@Entity
@Table(name = "archives")
public class Archive {

    /** 存档ID，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 存档标题 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 分类：survival / build / redstone / adventure / minigame / modpack / skyblock / other */
    @Column(nullable = false, length = 20)
    private String category;

    /** MC 游戏版本，如 1.21 / 1.20.4 / 1.12.2 */
    @Column(nullable = false, length = 20)
    private String mcVersion;

    /** Mod 加载器：vanilla / fabric / forge / neoforge / quilt */
    @Column(nullable = false, length = 20)
    private String modLoader;

    /** 存档详细介绍，最大2000字 */
    @Column(nullable = false, length = 2000)
    private String description;

    /** 点赞数（冗余字段，避免 COUNT 查询） */
    @Column(nullable = false)
    private int likeCount = 0;

    /** 下载次数 */
    @Column(nullable = false)
    private int downloadCount = 0;

    /** 踩数 */
    @Column(nullable = false)
    private int dislikeCount = 0;

    /** 浏览次数 */
    @Column(nullable = false)
    private int viewCount = 0;

    /** 存档文件在服务器上的存储路径 */
    @Column(length = 500)
    private String filePath;

    /** 存档文件大小（字节） */
    @Column
    private Long fileSize;

    /** 发布者 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** 发布时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 存档的展示图片列表 */
    @OneToMany(mappedBy = "archive", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ArchiveImage> images = new ArrayList<>();

    // ===== Getter / Setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMcVersion() { return mcVersion; }
    public void setMcVersion(String mcVersion) { this.mcVersion = mcVersion; }

    public String getModLoader() { return modLoader; }
    public void setModLoader(String modLoader) { this.modLoader = modLoader; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    public int getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ArchiveImage> getImages() { return images; }
    public void setImages(List<ArchiveImage> images) { this.images = images; }
}
