package com.mcarchive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 创建存档请求 DTO
 * 
 * 取代 ArchiveService.createArchive() 的 7 个零散参数，
 * 后续加字段只需加 DTO 属性，不改变方法签名。
 */
public class CreateArchiveRequest {

    @NotBlank(message = "存档名称不能为空")
    @Size(max = 100, message = "名称最长 100 字")
    private String title;

    @NotBlank(message = "请选择分类")
    private String category;

    @NotBlank(message = "请选择 MC 版本")
    private String mcVersion;

    @NotBlank(message = "请选择 Mod 加载器")
    private String modLoader;

    @NotBlank(message = "介绍不能为空")
    @Size(min = 10, max = 2000, message = "介绍 10~2000 字")
    private String description;

    private MultipartFile file;

    private List<MultipartFile> images;

    // ===== Getter / Setter =====
    public String getTitle()       { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getCategory()    { return category; }
    public void setCategory(String c) { this.category = c; }
    public String getMcVersion()   { return mcVersion; }
    public void setMcVersion(String v) { this.mcVersion = v; }
    public String getModLoader()   { return modLoader; }
    public void setModLoader(String l) { this.modLoader = l; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile f) { this.file = f; }
    public List<MultipartFile> getImages() { return images; }
    public void setImages(List<MultipartFile> imgs) { this.images = imgs; }
}
