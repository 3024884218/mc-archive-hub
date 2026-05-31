package com.mcarchive.repository;

import com.mcarchive.model.Follow;
import com.mcarchive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 关注数据访问层
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    /** 检查是否已关注 */
    boolean existsByFollowerAndFollowing(User follower, User following);

    /** 取消关注 */
    @Modifying
    void deleteByFollowerAndFollowing(User follower, User following);

    /** 获取用户的关注列表（我关注了谁） */
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId ORDER BY f.createdAt DESC")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);

    /** 获取用户的粉丝列表（谁关注了我） */
    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId ORDER BY f.createdAt DESC")
    List<Long> findFollowerIdsByUserId(@Param("userId") Long userId);

    /** 统计关注数 */
    long countByFollower(User user);

    /** 统计粉丝数 */
    long countByFollowing(User user);
}
