package com.mcarchive.repository;

import com.mcarchive.model.Archive;
import com.mcarchive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 收藏记录数据访问层
 */
@Repository
public interface BookmarkRepository extends JpaRepository<BookmarkRecord, Long> {

    boolean existsByUserAndArchive(User user, Archive archive);

    void deleteByUserAndArchive(User user, Archive archive);

    /** 查询用户收藏的所有存档ID（按收藏时间倒序） */
    @Query("SELECT b.archive.id FROM BookmarkRecord b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Long> findArchiveIdsByUserId(@Param("userId") Long userId);
}
