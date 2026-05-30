package com.mcarchive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务 — 管理存档文件和图片的本地磁盘存储
 *
 * 存储结构：
 *   {app.upload.dir}/
 *     archives/{archiveId}/
 *       archive.zip          ← 存档文件
 *       images/
 *         {uuid}.jpg         ← 展示图片
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    /** 允许的存档文件扩展名 */
    private static final Set<String> ALLOWED_ARCHIVE_EXTENSIONS = Set.of(
        ".zip", ".rar", ".7z"
    );

    /** 图片最大大小：10MB */
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadRoot);
            log.info("上传目录已就绪: {}", uploadRoot.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadDir, e);
        }
    }

    /**
     * 存储存档文件
     * @param archiveId 存档ID
     * @param file 上传的文件
     * @return 文件存储的相对路径
     * @throws IllegalArgumentException 文件类型不合法时
     */
    public String storeArchiveFile(Long archiveId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("存档文件不能为空");
        }

        // 扩展名白名单校验
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_ARCHIVE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                "不支持的存档格式: " + extension + "，仅支持 .zip / .rar / .7z");
        }

        Path archiveDir = getArchiveDir(archiveId);
        Files.createDirectories(archiveDir);

        String fileName = "archive" + extension.toLowerCase();
        Path targetPath = archiveDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("存档文件已存储: archiveId={}, size={}, path={}", archiveId, file.getSize(), targetPath);
        return "archives/" + archiveId + "/" + fileName;
    }

    /**
     * 存储展示图片
     * @param archiveId 存档ID
     * @param file 上传的图片
     * @return 图片的相对路径
     * @throws IllegalArgumentException 文件类型/大小不合法时
     */
    public String storeImage(Long archiveId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }

        // 大小限制
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                "图片大小不能超过 10MB，当前: " + (file.getSize() / 1024 / 1024) + "MB");
        }

        // 扩展名白名单校验
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                "不支持的图片格式: " + extension + "，仅支持 .jpg / .png / .webp / .gif");
        }

        Path imagesDir = getArchiveDir(archiveId).resolve("images");
        Files.createDirectories(imagesDir);

        String fileName = UUID.randomUUID().toString() + extension.toLowerCase();
        Path targetPath = imagesDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.debug("图片已存储: archiveId={}, fileName={}", archiveId, fileName);
        return "archives/" + archiveId + "/images/" + fileName;
    }

    /**
     * 获取存档文件的完整路径
     */
    public Path getArchiveFilePath(String relativePath) {
        return uploadRoot.resolve(relativePath);
    }

    /**
     * 删除存档的所有文件（异步执行，不阻塞主流程）
     * 删除失败时记录日志但不抛异常 — 文件泄漏比功能中断危害更小
     */
    public void deleteArchiveFiles(Long archiveId) {
        try {
            Path archiveDir = getArchiveDir(archiveId);
            if (Files.exists(archiveDir)) {
                deleteRecursively(archiveDir);
                log.info("存档文件已删除: archiveId={}", archiveId);
            }
        } catch (IOException e) {
            log.warn("删除存档文件失败: archiveId={}, error={}", archiveId, e.getMessage());
        }
    }

    // ===== 内部方法 =====

    private Path getArchiveDir(Long archiveId) {
        return uploadRoot.resolve("archives").resolve(String.valueOf(archiveId));
    }

    /**
     * 递归删除目录及内容，失败时记录日志
     */
    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                entries.forEach(p -> {
                    try {
                        deleteRecursively(p);
                    } catch (IOException e) {
                        log.warn("递归删除文件失败: {}, error={}", p, e.getMessage());
                    }
                });
            }
        }
        Files.deleteIfExists(path);
    }

    /**
     * 从文件名提取扩展名（含点号），例如 "image.PNG" → ".png"
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    // ===== 用户头像 =====

    /**
     * 存储用户头像
     * @param userId 用户ID
     * @param file 上传的头像文件
     * @return 头像的相对路径
     */
    public String storeAvatar(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("头像文件不能为空");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("头像大小不能超过 5MB");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的图片格式: " + extension);
        }

        Path avatarDir = uploadRoot.resolve("avatars");
        Files.createDirectories(avatarDir);

        // 固定文件名：{userId}.jpg，覆盖旧头像
        String fileName = userId + ".jpg";
        Path targetPath = avatarDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("头像已存储: userId={}, path={}", userId, targetPath);
        return "avatars/" + fileName;
    }

    // ===== 用户收款码 =====

    /**
     * 存储用户收款二维码（微信/支付宝）
     * @param userId 用户ID
     * @param type "wx" 或 "ali"
     * @param file 上传的二维码图片
     * @return 图片的相对路径
     */
    public String storeQrCode(Long userId, String type, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("二维码文件不能为空");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("二维码图片不能超过 2MB");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的图片格式: " + extension);
        }

        Path qrDir = uploadRoot.resolve("qrcodes");
        Files.createDirectories(qrDir);

        String fileName = userId + "_" + type + ".jpg";
        Path targetPath = qrDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("收款码已存储: userId={}, type={}, path={}", userId, type, targetPath);
        return "qrcodes/" + fileName;
    }
}
