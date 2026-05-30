package com.mcarchive.repository;

import com.mcarchive.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 按存档查询评论，按时间倒序 */
    List<Comment> findByArchiveIdOrderByCreatedAtDesc(Long archiveId);

    /** 统计存档评论数 */
    int countByArchiveId(Long archiveId);

    /** 删除存档的所有评论 */
    void deleteByArchiveId(Long archiveId);
}
