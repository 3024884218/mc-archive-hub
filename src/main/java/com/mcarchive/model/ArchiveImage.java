package com.mcarchive.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * 存档展示图片实体
 * 每个存档可以有多张展示图片
 */
@Entity
@Table(name = "archive_images")
public class ArchiveImage {

    /** 图片ID，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属存档 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id", nullable = false)
    @JsonIgnore
    private Archive archive;

    /** 图片文件在服务器上的存储路径 */
    @Column(nullable = false, length = 500)
    private String imagePath;

    /** 排序顺序（0=封面） */
    @Column(nullable = false)
    private int sortOrder;

    // ===== Getter / Setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Archive getArchive() { return archive; }
    public void setArchive(Archive archive) { this.archive = archive; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
