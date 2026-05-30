package com.mcarchive.controller;

import com.mcarchive.dto.CreateArchiveRequest;
import com.mcarchive.model.Archive;
import com.mcarchive.model.ArchiveImage;
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

    /** 全文搜索 */
    @GetMapping("/search")
    public ResponseEntity<?> searchArchives(@RequestParam("q") String q) {
        List<Archive> archives = archiveService.searchArchives(q);
        CustomUserDetails details = currentUser.getDetails();

        Set<Long> likedIds = details != null
            ? new HashSet<>(archiveService.getLikedArchiveIds(details.getUser())) : Set.of();
        Set<Long> bmIds = details != null
            ? new HashSet<>(archiveService.getBookmarkedArchiveIds(details.getUser())) : Set.of();

        return ResponseEntity.ok(archives.stream()
            .map(a -> archiveToMap(a, likedIds.contains(a.getId()), bmIds.contains(a.getId())))
            .toList());
    }

    /** 元数据 — 现有存档的动态分类/版本/加载器 */
    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadata() {
        return ResponseEntity.ok(archiveService.getMetadata());
    }

    /** 存档列表（支持筛选） */
    @GetMapping
    public ResponseEntity<?> listArchives(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String mcVersion,
            @RequestParam(required = false) String modLoader,
            @RequestParam(defaultValue = "popular") String sort) {

        boolean sortPopular = !"newest".equals(sort);
        List<Archive> archives = archiveService.getArchivesByFilters(category, mcVersion, modLoader, sortPopular);
        CustomUserDetails details = currentUser.getDetails();

        Set<Long> likedIds = details != null
            ? new HashSet<>(archiveService.getLikedArchiveIds(details.getUser()))
            : Set.of();
        Set<Long> bmIds = details != null
            ? new HashSet<>(archiveService.getBookmarkedArchiveIds(details.getUser()))
            : Set.of();

        return ResponseEntity.ok(archives.stream()
            .map(a -> archiveToMap(a, likedIds.contains(a.getId()), bmIds.contains(a.getId())))
            .toList());
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

        return ResponseEntity.ok(archiveToMap(archive, liked, bmed));
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

    // ===== 辅助 =====

    private User requireAuth() {
        return currentUser.require();
    }

    private Map<String, Object> archiveToMap(Archive a) {
        return archiveToMap(a, false, false);
    }

    private Map<String, Object> archiveToMap(Archive a, boolean liked, boolean bookmarked) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("title", a.getTitle());
        map.put("category", a.getCategory());
        map.put("mcVersion", a.getMcVersion());
        map.put("modLoader", a.getModLoader());
        map.put("description", a.getDescription());
        map.put("likeCount", a.getLikeCount());
        map.put("downloadCount", a.getDownloadCount());
        map.put("viewCount", a.getViewCount());
        map.put("liked", liked);
        map.put("bookmarked", bookmarked);
        map.put("createdAt", a.getCreatedAt().toString());
        if (a.getAuthor() != null) {
            map.put("authorName", a.getAuthor().getNickname());
            map.put("authorId", a.getAuthor().getId());
            map.put("wechatQrCodeUrl", a.getAuthor().getWechatQrCode() != null
                ? "/uploads/" + a.getAuthor().getWechatQrCode() : null);
            map.put("alipayQrCodeUrl", a.getAuthor().getAlipayQrCode() != null
                ? "/uploads/" + a.getAuthor().getAlipayQrCode() : null);
        }
        List<Map<String, Object>> imgs = new ArrayList<>();
        for (ArchiveImage img : a.getImages()) {
            imgs.add(Map.of("id", img.getId(), "url", "/uploads/" + img.getImagePath(), "sortOrder", img.getSortOrder()));
        }
        map.put("images", imgs);
        map.put("hasFile", a.getFilePath() != null && !a.getFilePath().isEmpty());
        return map;
    }
}
