package com.mcarchive.repository;

import com.mcarchive.model.Archive;
import com.mcarchive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 点赞记录数据访问层
 * 存储 user_id -> archive_id 的点赞关系
 */
@Repository
public interface LikeRepository extends JpaRepository<LikeRecord, Long> {

    /** 检查用户是否已点赞某存档 */
    boolean existsByUserAndArchive(User user, Archive archive);

    /** 取消点赞 */
    void deleteByUserAndArchive(User user, Archive archive);

    /** 统计某存档的点赞数 */
    long countByArchive(Archive archive);

    /** 查询用户点赞的所有存档ID */
    @Query("SELECT l.archive.id FROM LikeRecord l WHERE l.user.id = :userId")
    List<Long> findArchiveIdsByUserId(@Param("userId") Long userId);
}
