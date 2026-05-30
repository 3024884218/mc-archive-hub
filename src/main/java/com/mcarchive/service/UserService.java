package com.mcarchive.service;

import com.mcarchive.model.User;
import com.mcarchive.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户服务 — 注册、登录、邮箱绑定、密码重置
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int VERIFY_TOKEN_HOURS = 24;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * 用户注册（含邮箱）
     * @throws IllegalArgumentException 用户名/邮箱已存在时
     */
    @Transactional
    public User register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setCreatedAt(LocalDateTime.now());

        if (email != null && !email.isBlank()) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("该邮箱已被其他账号绑定");
            }
            user.setEmail(email);
            user.setEmailVerified(false);
            user.setEmailVerificationToken(UUID.randomUUID().toString().replace("-", ""));
            user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(VERIFY_TOKEN_HOURS));
        }

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 并发冲突时给明确错误
            String cause = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage() : "";
            if (cause.contains("users.username") || cause.contains("username")) {
                throw new IllegalArgumentException("用户名已被注册");
            }
            if (cause.contains("users.email") || cause.contains("email")) {
                throw new IllegalArgumentException("该邮箱已被其他账号绑定");
            }
            throw new IllegalArgumentException("操作冲突，请稍后重试");
        }
    }

    /**
     * 验证用户登录
     * @return 验证成功返回用户，失败返回 null
     * @throws IllegalArgumentException 账号已被锁定时
     */
    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return null;
        }

        // 检查账号锁定
        if (isLocked(user)) {
            throw new IllegalArgumentException("账号已被临时锁定，请" + LOCK_MINUTES + "分钟后再试");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 记录失败次数
            recordFailedAttempt(user);
            return null;
        }

        // 登录成功 — 清零失败计数
        if (user.getFailedLoginAttempts() > 0 || user.getAccountLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }
        return user;
    }

    private boolean isLocked(User user) {
        if (user.getAccountLockedUntil() == null) return false;
        if (LocalDateTime.now().isAfter(user.getAccountLockedUntil())) {
            // 锁定已过期
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            return false;
        }
        return true;
    }

    private void recordFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            log.warn("账号 {} 因 {} 次登录失败被锁定 {} 分钟", user.getUsername(), attempts, LOCK_MINUTES);
        }
        userRepository.save(user);
    }

    /**
     * 按 ID 查找用户
     */
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 按用户名查找用户
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 按邮箱查找用户
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ===== 邮箱绑定 =====

    /**
     * 绑定邮箱 — 生成验证令牌并发送验证邮件
     */
    @Transactional
    public void bindEmail(User user, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("该邮箱已被其他账号绑定");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        user.setEmail(email);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(VERIFY_TOKEN_HOURS));
        userRepository.save(user);

        emailService.sendVerificationEmail(email, user.getUsername(), token);
    }

    /**
     * 验证邮箱 — 用令牌验证
     * @return 验证成功返回 true
     */
    @Transactional
    public boolean verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token).orElse(null);
        if (user == null) {
            return false;
        }

        // 检查是否过期
        if (user.getEmailVerificationTokenExpiry() != null
                && user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setEmailVerificationToken(null);
            user.setEmailVerificationTokenExpiry(null);
            userRepository.save(user);
            return false;
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);
        return true;
    }

    // ===== 密码重置 =====

    /**
     * 请求密码重置 — 向绑定邮箱发送重置链接
     */
    @Transactional
    public void requestPasswordReset(String email) {
        User user = findByEmail(email).orElse(null);
        if (user == null) {
            // 不暴露邮箱是否注册，一律显示"已发送"
            return;
        }
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("该邮箱尚未验证，请先验证邮箱");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(email, user.getUsername(), token);
    }

    /**
     * 通过令牌重置密码
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token).orElse(null);
        if (user == null) return false;

        // 检查令牌是否过期
        if (user.getPasswordResetTokenExpiry() == null
                || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            // 清理过期令牌
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            userRepository.save(user);
            return false;
        }

        // 重置密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
        return true;
    }

    // ===== 个人资料 =====

    /**
     * 更新昵称
     */
    @Transactional
    public void updateProfile(User user, String nickname) {
        user.setNickname(nickname);
        userRepository.save(user);
    }

    /**
     * 修改密码（需要旧密码验证）
     * @return true = 成功, false = 旧密码错误
     */
    @Transactional
    public boolean changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    /**
     * 保存用户（通用方法，如头像路径更新等）
     */
    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }
}
