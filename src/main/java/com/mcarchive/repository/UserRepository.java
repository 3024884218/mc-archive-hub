package com.mcarchive.repository;

import com.mcarchive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 按用户名查找用户 */
    Optional<User> findByUsername(String username);

    /** 判断用户名是否已存在 */
    boolean existsByUsername(String username);

    /** 按邮箱查找用户 */
    Optional<User> findByEmail(String email);

    /** 判断邮箱是否已被绑定 */
    boolean existsByEmail(String email);

    /** 按邮箱验证令牌查找用户 */
    Optional<User> findByEmailVerificationToken(String token);

    /** 按密码重置令牌查找用户 */
    Optional<User> findByPasswordResetToken(String token);
}
