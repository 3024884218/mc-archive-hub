package com.mcarchive.controller;

import com.mcarchive.dto.CreateArchiveRequest;
import com.mcarchive.model.Archive;
import com.mcarchive.model.ArchiveImage;
import com.mcarchive.model.Comment;
import com.mcarchive.model.User;
import com.mcarchive.security.CurrentUser;
import com.mcarchive.security.CustomUserDetails;
import com.mcarchive.service.ArchiveService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 存档控制器 — 重构版
 * - 用 @Valid CreateArchiveRequest 替代散参数
 * - 用 CustomUserDetails 获取当前用户
 * - 上传路径从 application.properties 注入
 */
@RestController
@RequestMapping("/api/archives")
public class ArchiveController {

    private final ArchiveService archiveService;
    private final Path uploadRoot;
    private final CurrentUser currentUser;

    public ArchiveController(ArchiveService archiveService,
                              @Value("${app.upload.dir}") String uploadDir,
                              CurrentUser currentUser) {
        this.archiveService = archiveService;
        this.uploadRoot = Paths.get(uploadDir);
        this.currentUser = currentUser;
    }

    /** 发布新存档 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createArchive(@Valid @ModelAttribute CreateArchiveRequest req) {
        User user = requireAuth();

        Archive archive = archiveService.createArchive(user, req);
        return ResponseEntity.ok(Map.of("message", "发布成功", "archive", archiveToMap(archive)));
    }

    /** 全文搜索（支持分页） */
    @GetMapping("/search")
    public ResponseEntity<?> searchArchives(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Archive> paged = archiveService.searchArchivesPaged(q, page, size);
        CustomUserDetails details = currentUser.getDetails();

        Set<Long> likedIds = details != null
            ? new HashSet<>(archiveService.getLikedArchiveIds(details.getUser())) : Set.of();
        Set<Long> bmIds = details != null
            ? new HashSet<>(archiveService.getBookmarkedArchiveIds(details.getUser())) : Set.of();

        List<Map<String, Object>> items = paged.getContent().stream()
            .map(a -> archiveToMap(a, likedIds.contains(a.getId()), bmIds.contains(a.getId())))
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", items);
        result.put("totalElements", paged.getTotalElements());
        result.put("totalPages", paged.getTotalPages());
        result.put("currentPage", paged.getNumber());
        result.put("size", paged.getSize());
        return ResponseEntity.ok(result);
    }

    /** 元数据 — 现有存档的动态分类/版本/加载器 */
    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadata() {
        return ResponseEntity.ok(archiveService.getMetadata());
    }

    /** 存档列表（支持筛选 + 分页） */
    @GetMapping
    public ResponseEntity<?> listArchives(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String mcVersion,
            @RequestParam(required = false) String modLoader,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Page<Archive> paged = archiveService.getArchivesByFiltersPaged(
            category, mcVersion, modLoader, sort, page, size);
        CustomUserDetails details = currentUser.getDetails();

        Set<Long> likedIds = details != null
            ? new HashSet<>(archiveService.getLikedArchiveIds(details.getUser()))
            : Set.of();
        Set<Long> bmIds = details != null
            ? new HashSet<>(archiveService.getBookmarkedArchiveIds(details.getUser()))
            : Set.of();

        List<Map<String, Object>> items = paged.getContent().stream()
            .map(a -> archiveToMap(a, likedIds.contains(a.getId()), bmIds.contains(a.getId())))
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", items);
        result.put("totalElements", paged.getTotalElements());
        result.put("totalPages", paged.getTotalPages());
        result.put("currentPage", paged.getNumber());
        result.put("size", paged.getSize());
        return ResponseEntity.ok(result);
    }

    /** 存档详情 */
    @GetMapping("/{id}")
    public ResponseEntity<?> getArchive(@PathVariable Long id) {
        Archive archive = archiveService.getArchiveById(id);
        if (archive == null) return ResponseEntity.notFound().build();

        // 记录浏览
        archiveService.incrementViewCount(id);

        CustomUserDetails details = currentUser.getDetails();
        boolean liked = details != null && archiveService.isLiked(details.getUser(), id);
        boolean bmed  = details != null && archiveService.isBookmarked(details.getUser(), id);
        boolean disliked = details != null && archiveService.isDisliked(details.getUser(), id);

        return ResponseEntity.ok(archiveToMap(archive, liked, bmed, disliked));
    }

    /** 相关存档（同分类，排除自身，最多4个） */
    @GetMapping("/{id}/related")
    public ResponseEntity<?> getRelatedArchives(@PathVariable Long id) {
        Archive archive = archiveService.getArchiveById(id);
        if (archive == null) return ResponseEntity.notFound().build();

        List<Archive> related = archiveService.getRelatedArchives(archive.getCategory(), id, 4);
        return ResponseEntity.ok(related.stream().map(this::archiveToMap).toList());
    }

    /** 切换点赞 */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id) {
        User user = requireAuth();
        boolean liked = archiveService.toggleLike(user, id);
        Archive a = archiveService.getArchiveById(id);
        if (a == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("liked", liked, "likeCount", a.getLikeCount()));
    }

    /** 切换收藏 */
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<?> toggleBookmark(@PathVariable Long id) {
        User user = requireAuth();
        boolean bmed = archiveService.toggleBookmark(user, id);
        return ResponseEntity.ok(Map.of("bookmarked", bmed));
    }

    /** 编辑存档（仅作者） */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateArchive(@PathVariable Long id,
                                            @Valid @ModelAttribute CreateArchiveRequest req) {
        User user = requireAuth();
        Archive archive = archiveService.updateArchive(user, id, req);
        if (archive == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "存档已更新", "archive", archiveToMap(archive)));
    }

    /** 删除存档（仅作者） */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArchive(@PathVariable Long id) {
        User user = requireAuth();
        boolean ok = archiveService.deleteArchive(user, id);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "存档已删除"));
    }

    /** 下载存档 — 使用 RFC 5987 编码文件名，支持中文 */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadArchive(@PathVariable Long id) {
        Archive archive = archiveService.getArchiveById(id);
        if (archive == null || archive.getFilePath() == null)
            return ResponseEntity.notFound().build();

        archiveService.incrementDownloadCount(id);
        // 记录下载历史
        CustomUserDetails details = currentUser.getDetails();
        if (details != null) {
            archiveService.recordDownload(details.getUser().getId(), id);
        }

        Resource resource = new FileSystemResource(uploadRoot.resolve(archive.getFilePath()));
        if (!resource.exists()) return ResponseEntity.notFound().build();

        // RFC 5987: filename*=UTF-8''encoded_filename
        String encodedFilename = URLEncoder.encode(archive.getTitle(), StandardCharsets.UTF_8)
            .replace("+", "%20");
        // 同时提供 ASCII fallback: filename="archive.zip"
        String contentDisposition = "attachment; filename=\"archive.zip\"; "
            + "filename*=UTF-8''" + encodedFilename + ".zip";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    /** 查看某作者的存档（分页） */
    @GetMapping("/author/{authorId}")
    public ResponseEntity<?> getArchivesByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Archive> paged = archiveService.getArchivesByAuthorPaged(authorId, page, size);
        CustomUserDetails details = currentUser.getDetails();

        Set<Long> likedIds = details != null
            ? new HashSet<>(archiveService.getLikedArchiveIds(details.getUser())) : Set.of();
        Set<Long> bmIds = details != null
            ? new HashSet<>(archiveService.getBookmarkedArchiveIds(details.getUser())) : Set.of();

        List<Map<String, Object>> items = paged.getContent().stream()
            .map(a -> archiveToMap(a, likedIds.contains(a.getId()), bmIds.contains(a.getId())))
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", items);
        result.put("totalElements", paged.getTotalElements());
        result.put("totalPages", paged.getTotalPages());
        result.put("currentPage", paged.getNumber());
        result.put("size", paged.getSize());
        return ResponseEntity.ok(result);
    }

    /** 我的下载历史 */
    @GetMapping("/my/downloads")
    public ResponseEntity<?> getMyDownloads() {
        User user = requireAuth();
        var archives = archiveService.getDownloadedArchives(user.getId());
        return ResponseEntity.ok(archives.stream()
            .map(a -> archiveToMap(a,
                archiveService.isLiked(user, a.getId()),
                archiveService.isBookmarked(user, a.getId())))
            .toList());
    }

    /** 我的存档 */
    @GetMapping("/my/list")
    public ResponseEntity<?> getMyArchives() {
        User user = requireAuth();
        var archives = archiveService.getArchivesByAuthor(user.getId());
        return ResponseEntity.ok(archives.stream()
            .map(a -> archiveToMap(a,
                archiveService.isLiked(user, a.getId()),
                archiveService.isBookmarked(user, a.getId())))
            .toList());
    }

    /** 我的收藏 */
    @GetMapping("/my/bookmarks")
    public ResponseEntity<?> getMyBookmarks() {
        User user = requireAuth();
        var ids = archiveService.getBookmarkedArchiveIds(user);
        return ResponseEntity.ok(ids.stream()
            .map(archiveService::getArchiveById)
            .filter(Objects::nonNull)
            .map(a -> archiveToMap(a, archiveService.isLiked(user, a.getId()), true))
            .toList());
    }

    /** 切换踩 */
    @PostMapping("/{id}/dislike")
    public ResponseEntity<?> toggleDislike(@PathVariable Long id) {
        User user = requireAuth();
        boolean disliked = archiveService.toggleDislike(user, id);
        Archive a = archiveService.getArchiveById(id);

        if (a == null) {
            return ResponseEntity.ok(Map.of("disliked", disliked, "deleted", true,
                "message", "该存档因踩数过多已被自动删除"));
        }
        return ResponseEntity.ok(Map.of("disliked", disliked, "dislikeCount", a.getDislikeCount()));
    }

    /** 获取存档评论 */
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        List<Comment> comments = archiveService.getComments(id);
        return ResponseEntity.ok(comments.stream().map(this::commentToMap).toList());
    }

    /** 发表评论 */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        User user = requireAuth();
        String content = body.get("content");
        try {
            Comment comment = archiveService.addComment(user, id, content);
            return ResponseEntity.ok(commentToMap(comment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 获取未读通知数 */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<?> getUnreadNotificationCount() {
        User user = requireAuth();
        long count = archiveService.getUnreadNotificationCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /** 获取通知列表 */
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        User user = requireAuth();
        var notifications = archiveService.getNotifications(user.getId());
        return ResponseEntity.ok(notifications.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("archiveId", n.getArchiveId());
            m.put("actorName", n.getActorName());
            m.put("archiveTitle", n.getArchiveTitle());
            m.put("read", n.isRead());
            m.put("createdAt", n.getCreatedAt().toString());
            return m;
        }).toList());
    }

    /** 标记所有通知为已读 */
    @PostMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsRead() {
        User user = requireAuth();
        archiveService.markAllNotificationsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    /** 删除评论 */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        User user = requireAuth();
        boolean ok = archiveService.deleteComment(user, commentId);
        if (ok) return ResponseEntity.ok(Map.of("message", "评论已删除"));
        return ResponseEntity.badRequest().body(Map.of("error", "无权删除此评论"));
    }

    // ===== 辅助 =====

    private User requireAuth() {
        return currentUser.require();
    }

    private Map<String, Object> archiveToMap(Archive a) {
        return archiveToMap(a, false, false, false);
    }

    private Map<String, Object> archiveToMap(Archive a, boolean liked, boolean bookmarked) {
        return archiveToMap(a, liked, bookmarked, false);
    }

    private Map<String, Object> archiveToMap(Archive a, boolean liked, boolean bookmarked, boolean disliked) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("title", a.getTitle());
        map.put("category", a.getCategory());
        map.put("mcVersion", a.getMcVersion());
        map.put("modLoader", a.getModLoader());
        map.put("description", a.getDescription());
        map.put("likeCount", a.getLikeCount());
        map.put("downloadCount", a.getDownloadCount());
        map.put("dislikeCount", a.getDislikeCount());
        map.put("viewCount", a.getViewCount());
        map.put("liked", liked);
        map.put("bookmarked", bookmarked);
        map.put("disliked", disliked);
        map.put("createdAt", a.getCreatedAt().toString());
        if (a.getAuthor() != null) {
            map.put("authorName", a.getAuthor().getNickname());
            map.put("authorId", a.getAuthor().getId());
            map.put("wechatQrCodeUrl", a.getAuthor().getWechatQrCode() != null
                ? "/uploads/" + a.getAuthor().getWechatQrCode() : null);
            map.put("alipayQrCodeUrl", a.getAuthor().getAlipayQrCode() != null
                ? "/uploads/" + a.getAuthor().getAlipayQrCode() : null);
            map.put("contactEmail", a.getAuthor().getContactEmail());
        }
        List<Map<String, Object>> imgs = new ArrayList<>();
        for (ArchiveImage img : a.getImages()) {
            imgs.add(Map.of("id", img.getId(), "url", "/uploads/" + img.getImagePath(), "sortOrder", img.getSortOrder()));
        }
        map.put("images", imgs);
        map.put("hasFile", a.getFilePath() != null && !a.getFilePath().isEmpty());
        map.put("fileSize", a.getFileSize());
        return map;
    }

    private Map<String, Object> commentToMap(Comment c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("content", c.getContent());
        m.put("createdAt", c.getCreatedAt().toString());
        if (c.getAuthor() != null) {
            m.put("authorName", c.getAuthor().getNickname());
            m.put("authorId", c.getAuthor().getId());
            m.put("authorAvatar", c.getAuthor().getAvatarPath() != null
                ? "/uploads/" + c.getAuthor().getAvatarPath() : null);
        }
        return m;
    }
}
