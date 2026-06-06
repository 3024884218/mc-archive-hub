package com.mcarchive.service;

import com.mcarchive.dto.CreateArchiveRequest;
import com.mcarchive.model.Archive;
import com.mcarchive.model.ArchiveImage;
import com.mcarchive.model.Comment;
import com.mcarchive.model.User;
import com.mcarchive.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存档服务 — 处理存档的 CRUD、点赞、收藏、下载
 */
@Service
public class ArchiveService {

    private final ArchiveRepository archiveRepository;
    private final LikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final DislikeRepository dislikeRepository;
    private final CommentRepository commentRepository;
    private final DownloadRecordRepository downloadRecordRepository;
    private final NotificationRepo notificationRepo;
    private final FileStorageService fileStorageService;

    public ArchiveService(ArchiveRepository archiveRepository,
                          LikeRepository likeRepository,
                          BookmarkRepository bookmarkRepository,
                          DislikeRepository dislikeRepository,
                          CommentRepository commentRepository,
                          DownloadRecordRepository downloadRecordRepository,
                          NotificationRepo notificationRepo,
                          FileStorageService fileStorageService) {
        this.archiveRepository = archiveRepository;
        this.likeRepository = likeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.dislikeRepository = dislikeRepository;
        this.commentRepository = commentRepository;
        this.downloadRecordRepository = downloadRecordRepository;
        this.notificationRepo = notificationRepo;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 创建新存档 — 先处理文件存储，再一次性保存数据库，避免两阶段 save 导致的不一致
     */
    @Transactional
    public Archive createArchive(User author, CreateArchiveRequest req) {
        Archive archive = new Archive();
        archive.setAuthor(author);
        archive.setTitle(req.getTitle());
        archive.setCategory(req.getCategory());
        archive.setMcVersion(req.getMcVersion());
        archive.setModLoader(req.getModLoader());
        archive.setModsJson(req.getModsJson());
        archive.setDownloadUrl(req.getDownloadUrl());
        archive.setDescription(req.getDescription());
        archive.setCreatedAt(LocalDateTime.now());
        archive.setLikeCount(0);
        archive.setDownloadCount(0);

        // 步骤1: 先保存存档记录以获取 ID
        archive = archiveRepository.saveAndFlush(archive);

        // 步骤2: 处理文件存储（在 DB 事务之外的文件 IO，异常时事务回滚会留下孤儿文件，
        // 但 saveAndFlush 确保 ID 已分配，避免空记录）
        try {
            MultipartFile archiveFile = req.getFile();
            if (archiveFile != null && !archiveFile.isEmpty()) {
                archive.setFilePath(fileStorageService.storeArchiveFile(archive.getId(), archiveFile));
                archive.setFileSize(archiveFile.getSize());
            }

            List<MultipartFile> images = req.getImages();
            if (images != null && !images.isEmpty()) {
                List<ArchiveImage> imageList = new ArrayList<>();
                int order = 0;
                for (MultipartFile img : images) {
                    if (img.isEmpty()) continue;
                    String imgPath = fileStorageService.storeImage(archive.getId(), img);
                    ArchiveImage ai = new ArchiveImage();
                    ai.setArchive(archive);
                    ai.setImagePath(imgPath);
                    ai.setSortOrder(order++);
                    imageList.add(ai);
                }
                archive.setImages(imageList);
            }

            // 处理 Mod 文件
            List<MultipartFile> modFiles = req.getModFiles();
            String modsJson = archive.getModsJson();
            if (modFiles != null && !modFiles.isEmpty() && modsJson != null) {
                modsJson = injectModFilePaths(modsJson, modFiles, archive.getId());
                archive.setModsJson(modsJson);
            }
        } catch (Exception e) {
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }

        return archiveRepository.save(archive);
    }

    /**
     * 获取所有存档（按时间倒序）
     */
    public List<Archive> getAllArchives() {
        return archiveRepository.findByFilters("", "", "", false);
    }

    /**
     * 按分类获取存档
     */
    public List<Archive> getArchivesByCategory(String category) {
        return archiveRepository.findByFilters(category, "", "", false);
    }

    /**
     * 多条件筛选存档
     */
    public List<Archive> getArchivesByFilters(String category, String mcVersion, String modLoader, boolean sortPopular) {
        String cat = (category == null || category.isEmpty() || "all".equals(category)) ? "" : category;
        String ver = (mcVersion == null || mcVersion.isEmpty() || "all".equals(mcVersion)) ? "" : mcVersion;
        String loader = (modLoader == null || modLoader.isEmpty() || "all".equals(modLoader)) ? "" : modLoader;
        return archiveRepository.findByFilters(cat, ver, loader, sortPopular);
    }

    /**
     * 获取元数据 — 现有存档的分类/版本/加载器列表和计数
     */
    public Map<String, Object> getMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("categories", archiveRepository.countByCategory());
        meta.put("mcVersions", archiveRepository.countByMcVersion());
        meta.put("modLoaders", archiveRepository.countByModLoader());
        return meta;
    }

    /**
     * 获取某用户的存档
     */
    public List<Archive> getArchivesByAuthor(Long authorId) {
        return archiveRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    /**
     * 按 ID 获取存档
     */
    public Archive getArchiveById(Long id) {
        return archiveRepository.findById(id).orElse(null);
    }

    /**
     * 全文搜索 — 标题、介绍、版本、加载器、分类
     */
    public List<Archive> searchArchives(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return archiveRepository.findByFilters("", "", "", false);
        }
        return archiveRepository.search(keyword.trim());
    }

    // ===== 分页查询（修复无分页问题） =====

    /**
     * 多条件筛选存档（分页版，支持 popular/newest/downloads 排序）
     */
    public Page<Archive> getArchivesByFiltersPaged(String category, String mcVersion,
                                                    String modLoader, String sort,
                                                    int page, int size) {
        String cat = (category == null || category.isEmpty() || "all".equals(category)) ? "" : category;
        String ver = (mcVersion == null || mcVersion.isEmpty() || "all".equals(mcVersion)) ? "" : mcVersion;
        String loader = (modLoader == null || modLoader.isEmpty() || "all".equals(modLoader)) ? "" : modLoader;
        String sortMode = (sort == null) ? "popular" : sort;
        Pageable pageable = PageRequest.of(page, size);
        return archiveRepository.findByFiltersPaged(cat, ver, loader, sortMode, pageable);
    }

    /**
     * 全文搜索（分页版）
     */
    public Page<Archive> searchArchivesPaged(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getArchivesByFiltersPaged("", "", "", "newest", page, size);
        }
        return archiveRepository.searchPaged(keyword.trim(), PageRequest.of(page, size));
    }

    /**
     * 获取某用户的存档（分页版）
     */
    public Page<Archive> getArchivesByAuthorPaged(Long authorId, int page, int size) {
        return archiveRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(page, size));
    }

    /**
     * 切换点赞状态 — 已点赞则取消，未点赞则点赞
     * 使用数据库唯一约束 + 原子 UPDATE 消除 TOCTOU 竞态条件
     * @return true = 已点赞，false = 已取消
     */
    @Transactional
    public boolean toggleLike(User user, Long archiveId) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive == null) return false;

        if (likeRepository.existsByUserAndArchive(user, archive)) {
            // 取消点赞：原子递减计数 + 删除记录
            likeRepository.deleteByUserAndArchive(user, archive);
            archiveRepository.decrementLikeCount(archiveId);
            return false;
        } else {
            // 点赞：先插入记录（依赖 UNIQUE(user_id, archive_id) 防重），再原子递增
            try {
                LikeRecord record = new LikeRecord(user, archive);
                likeRepository.saveAndFlush(record);
                archiveRepository.incrementLikeCount(archiveId);
                // 通知作者（不通知自己）
                if (!archive.getAuthor().getId().equals(user.getId())) {
                    createNotification(archive.getAuthor().getId(), "like", archiveId,
                        user.getNickname(), archive.getTitle());
                }
                return true;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 并发时记录已存在，说明已经被点赞，视为取消失败，返回已点赞状态
                return true;
            }
        }
    }

    /**
     * 切换收藏状态 — 同上，使用数据库唯一约束防重
     * @return true = 已收藏，false = 已取消
     */
    @Transactional
    public boolean toggleBookmark(User user, Long archiveId) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive == null) return false;

        if (bookmarkRepository.existsByUserAndArchive(user, archive)) {
            bookmarkRepository.deleteByUserAndArchive(user, archive);
            return false;
        } else {
            try {
                BookmarkRecord record = new BookmarkRecord(user, archive);
                bookmarkRepository.saveAndFlush(record);
                return true;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 并发时记录已存在
                return true;
            }
        }
    }

    /**
     * 判断用户是否已点赞
     */
    public boolean isLiked(User user, Long archiveId) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive == null) return false;
        return likeRepository.existsByUserAndArchive(user, archive);
    }

    /**
     * 判断用户是否已收藏
     */
    public boolean isBookmarked(User user, Long archiveId) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive == null) return false;
        return bookmarkRepository.existsByUserAndArchive(user, archive);
    }

    /**
     * 获取用户点赞的存档 ID 列表
     */
    public List<Long> getLikedArchiveIds(User user) {
        return likeRepository.findArchiveIdsByUserId(user.getId());
    }

    /**
     * 获取用户收藏的存档 ID 列表
     */
    public List<Long> getBookmarkedArchiveIds(User user) {
        return bookmarkRepository.findArchiveIdsByUserId(user.getId());
    }

    /**
     * 增加下载次数（原子操作，消除读-改-写竞态）
     */
    @Transactional
    public void incrementDownloadCount(Long archiveId) {
        archiveRepository.incrementDownloadCount(archiveId);
    }

    /** 记录下载历史 */
    @Transactional
    public void recordDownload(Long userId, Long archiveId) {
        if (!downloadRecordRepository.existsByUserIdAndArchiveId(userId, archiveId)) {
            try {
                downloadRecordRepository.saveAndFlush(new DownloadRecord(userId, archiveId));
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 并发重复，忽略
            }
        }
    }

    /** 获取用户下载历史 */
    public List<Archive> getDownloadedArchives(Long userId) {
        List<DownloadRecord> records = downloadRecordRepository.findByUserIdOrderByDownloadedAtDesc(userId);
        return records.stream()
            .map(r -> archiveRepository.findById(r.getArchiveId()).orElse(null))
            .filter(a -> a != null)
            .toList();
    }

    @Transactional
    public void incrementViewCount(Long archiveId) {
        archiveRepository.incrementViewCount(archiveId);
    }

    // ===== 编辑 + 删除 =====

    @Transactional
    public Archive updateArchive(User user, Long archiveId, CreateArchiveRequest req) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive == null || !archive.getAuthor().getId().equals(user.getId())) return null;

        archive.setTitle(req.getTitle());
        archive.setCategory(req.getCategory());
        archive.setMcVersion(req.getMcVersion());
        archive.setModLoader(req.getModLoader());
        archive.setModsJson(req.getModsJson());
        archive.setDownloadUrl(req.getDownloadUrl());
        archive.setDescription(req.getDescription());

        // 如果有新截图
        List<MultipartFile> images = req.getImages();
        if (images != null && !images.isEmpty()) {
            List<ArchiveImage> imageList = new ArrayList<>();
            int order = 0;
            for (MultipartFile img : images) {
                if (img.isEmpty()) continue;
                try {
                    String imgPath = fileStorageService.storeImage(archiveId, img);
                    ArchiveImage ai = new ArchiveImage();
                    ai.setArchive(archive);
                    ai.setImagePath(imgPath);
                    ai.setSortOrder(order++);
                    imageList.add(ai);
                } catch (IOException e) {
                    throw new RuntimeException("图片存储失败", e);
                }
            }
            // 替换全部图片
            archive.getImages().clear();
            archive.getImages().addAll(imageList);
        }

        // 处理 Mod 文件
        List<MultipartFile> modFiles = req.getModFiles();
        String modsJson = archive.getModsJson();
        if (modFiles != null && !modFiles.isEmpty() && modsJson != null) {
            modsJson = injectModFilePaths(modsJson, modFiles, archiveId);
            archive.setModsJson(modsJson);
        }

        return archiveRepository.save(archive);
    }

    @Transactional
    public boolean deleteArchive(User user, Long archiveId) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive == null || !archive.getAuthor().getId().equals(user.getId())) return false;

        fileStorageService.deleteArchiveFiles(archiveId);
        archiveRepository.delete(archive);
        return true;
    }

    /** 获取同分类的相关存档（排除自身，最多 limit 个） */
    public List<Archive> getRelatedArchives(String category, Long excludeId, int limit) {
        return archiveRepository.findByCategoryAndIdNot(category, excludeId,
            PageRequest.of(0, limit)).getContent();
    }

    // ===== 踩功能 =====

    /** 自动删除阈值：踩 >= 10 且 踩数 > 点赞数 × 2 */
    private static final int AUTO_DELETE_DISLIKE_MIN = 10;
    private static final double AUTO_DELETE_RATIO = 2.0;

    @Transactional
    public boolean toggleDislike(User user, Long archiveId) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive == null) return false;

        // 不能踩自己的存档
        if (archive.getAuthor().getId().equals(user.getId())) return false;

        if (dislikeRepository.existsByUserIdAndArchiveId(user.getId(), archiveId)) {
            dislikeRepository.deleteByUserIdAndArchiveId(user.getId(), archiveId);
            archiveRepository.decrementDislikeCount(archiveId);
            return false;
        } else {
            try {
                DislikeRecord record = new DislikeRecord(user.getId(), archiveId);
                dislikeRepository.saveAndFlush(record);
                archiveRepository.incrementDislikeCount(archiveId);

                // 刷新缓存数据，检查是否需要自动删除
                archiveRepository.flush();
                archive = archiveRepository.findById(archiveId).orElse(null);
                if (archive != null && shouldAutoDelete(archive)) {
                    deleteArchiveAutomatically(archive);
                    return true; // 踩成功且触发了自动删除
                }
                return true;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                return true; // 已踩过
            }
        }
    }

    private boolean shouldAutoDelete(Archive archive) {
        int dislikes = archive.getDislikeCount();
        int likes = archive.getLikeCount();
        return dislikes >= AUTO_DELETE_DISLIKE_MIN && dislikes > likes * AUTO_DELETE_RATIO;
    }

    private void deleteArchiveAutomatically(Archive archive) {
        commentRepository.deleteByArchiveId(archive.getId());
        fileStorageService.deleteArchiveFiles(archive.getId());
        archiveRepository.delete(archive);
    }

    public boolean isDisliked(User user, Long archiveId) {
        return dislikeRepository.existsByUserIdAndArchiveId(user.getId(), archiveId);
    }

    // ===== 评论功能 =====

    /** 对存档发表评论 */
    @Transactional
    public Comment addComment(User user, Long archiveId, String content) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive == null) throw new IllegalArgumentException("存档不存在");
        if (content == null || content.trim().isEmpty())
            throw new IllegalArgumentException("评论不能为空");
        if (content.trim().length() > 2000)
            throw new IllegalArgumentException("评论最多2000字");

        Comment comment = new Comment();
        comment.setAuthor(user);
        comment.setArchive(archive);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);

        // 通知作者（不通知自己）
        if (!archive.getAuthor().getId().equals(user.getId())) {
            createNotification(archive.getAuthor().getId(), "comment", archiveId,
                user.getNickname(), archive.getTitle());
        }

        return saved;
    }

    private void createNotification(Long userId, String type, Long archiveId,
                                     String actorName, String archiveTitle) {
        try {
            Notification n = new Notification(userId, type, archiveId, actorName, archiveTitle);
            notificationRepo.save(n);
        } catch (Exception e) {
            // 通知创建失败不影响主流程
        }
    }

    public long getUnreadNotificationCount(Long userId) {
        return notificationRepo.countByUserIdAndReadFalse(userId);
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAllNotificationsRead(Long userId) {
        var unread = notificationRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepo.saveAll(unread);
    }

    /** 获取存档的所有评论 */
    public List<Comment> getComments(Long archiveId) {
        return commentRepository.findByArchiveIdOrderByCreatedAtDesc(archiveId);
    }

    /** 删除评论（仅作者可删） */
    @Transactional
    public boolean deleteComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return false;
        if (!comment.getAuthor().getId().equals(user.getId())) return false;
        commentRepository.delete(comment);
        return true;
    }

    /**
     * 将上传的 Mod 文件路径注入 modsJson 数组。
     * modsJson 格式: [{"name":"...","url":"...","fileIdx":0}, ...]
     * modFiles 列表顺序与 fileIdx 对应
     */
    private String injectModFilePaths(String modsJson, List<MultipartFile> modFiles, Long archiveId) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(modsJson);
            if (!root.isArray()) return modsJson;

            for (int i = 0; i < root.size() && i < modFiles.size(); i++) {
                MultipartFile modFile = modFiles.get(i);
                if (modFile != null && !modFile.isEmpty()) {
                    try {
                        String path = fileStorageService.storeModFile(archiveId, modFile, i);
                        ((com.fasterxml.jackson.databind.node.ObjectNode) root.get(i)).put("filePath", path);
                    } catch (IOException e) {
                        // 单个 Mod 文件存储失败不阻断整体
                    }
                }
            }
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return modsJson;
        }
    }
}
