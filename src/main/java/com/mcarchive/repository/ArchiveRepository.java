package com.mcarchive.repository;

import com.mcarchive.model.Archive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 存档数据访问层
 */
@Repository
public interface ArchiveRepository extends JpaRepository<Archive, Long> {

    /** 多条件筛选，支持排序（popular = 按点赞数，默认按时间） */
    @Query("SELECT a FROM Archive a WHERE "
         + "(:category = '' OR a.category = :category) AND "
         + "(:mcVersion = '' OR a.mcVersion = :mcVersion) AND "
         + "(:modLoader = '' OR a.modLoader = :modLoader) "
         + "ORDER BY "
         + "CASE WHEN :sortPopular = true THEN a.likeCount ELSE 0 END DESC, "
         + "a.createdAt DESC")
    List<Archive> findByFilters(@Param("category") String category,
                                 @Param("mcVersion") String mcVersion,
                                 @Param("modLoader") String modLoader,
                                 @Param("sortPopular") boolean sortPopular);

    /** 全文搜索 */
    @Query("SELECT a FROM Archive a WHERE "
         + "LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
         + "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
         + "LOWER(a.mcVersion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
         + "LOWER(a.modLoader) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
         + "LOWER(a.category) LIKE LOWER(CONCAT('%', :keyword, '%')) "
         + "ORDER BY a.createdAt DESC")
    List<Archive> search(@Param("keyword") String keyword);

    // ===== 元数据：动态提取现有分类/版本/加载器 =====

    @Query("SELECT DISTINCT a.category FROM Archive a ORDER BY a.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT a.mcVersion FROM Archive a ORDER BY a.mcVersion")
    List<String> findDistinctMcVersions();

    @Query("SELECT DISTINCT a.modLoader FROM Archive a ORDER BY a.modLoader")
    List<String> findDistinctModLoaders();

    @Query("SELECT a.category, COUNT(a) FROM Archive a GROUP BY a.category")
    List<Object[]> countByCategory();

    @Query("SELECT a.mcVersion, COUNT(a) FROM Archive a GROUP BY a.mcVersion ORDER BY COUNT(a) DESC")
    List<Object[]> countByMcVersion();

    @Query("SELECT a.modLoader, COUNT(a) FROM Archive a GROUP BY a.modLoader ORDER BY COUNT(a) DESC")
    List<Object[]> countByModLoader();

    // ===== 原子计数更新（修复竞态条件） =====

    /** 原子递增点赞数 */
    @Modifying
    @Query("UPDATE Archive a SET a.likeCount = a.likeCount + 1 WHERE a.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    /** 原子递减点赞数（保证非负） */
    @Modifying
    @Query("UPDATE Archive a SET a.likeCount = a.likeCount - 1 WHERE a.id = :id AND a.likeCount > 0")
    int decrementLikeCount(@Param("id") Long id);

    /** 原子递增下载次数 */
    @Modifying
    @Query("UPDATE Archive a SET a.downloadCount = a.downloadCount + 1 WHERE a.id = :id")
    int incrementDownloadCount(@Param("id") Long id);

    /** 原子递增浏览次数 */
    @Modifying
    @Query("UPDATE Archive a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    int incrementViewCount(@Param("id") Long id);

    /** 原子递增踩数 */
    @Modifying
    @Query("UPDATE Archive a SET a.dislikeCount = a.dislikeCount + 1 WHERE a.id = :id")
    int incrementDislikeCount(@Param("id") Long id);

    /** 原子递减踩数（保证非负） */
    @Modifying
    @Query("UPDATE Archive a SET a.dislikeCount = a.dislikeCount - 1 WHERE a.id = :id AND a.dislikeCount > 0")
    int decrementDislikeCount(@Param("id") Long id);

    // ===== 分页查询 =====

    @Query("SELECT a FROM Archive a WHERE "
         + "(:category = '' OR a.category = :category) AND "
         + "(:mcVersion = '' OR a.mcVersion = :mcVersion) AND "
         + "(:modLoader = '' OR a.modLoader = :modLoader) "
         + "ORDER BY "
         + "CASE WHEN :sortPopular = true THEN a.likeCount ELSE 0 END DESC, "
         + "a.createdAt DESC")
    Page<Archive> findByFiltersPaged(@Param("category") String category,
                                      @Param("mcVersion") String mcVersion,
                                      @Param("modLoader") String modLoader,
                                      @Param("sortPopular") boolean sortPopular,
                                      Pageable pageable);

    @Query("SELECT a FROM Archive a WHERE "
         + "LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
         + "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
         + "LOWER(a.mcVersion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
         + "LOWER(a.modLoader) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
         + "LOWER(a.category) LIKE LOWER(CONCAT('%', :keyword, '%')) "
         + "ORDER BY a.createdAt DESC")
    Page<Archive> searchPaged(@Param("keyword") String keyword, Pageable pageable);

    Page<Archive> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    // ===== 用户相关 =====
    List<Archive> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /** 同分类相关存档（排除自身） */
    Page<Archive> findByCategoryAndIdNot(String category, Long id, Pageable pageable);
}
