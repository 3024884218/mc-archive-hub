package com.mcarchive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 存储注册用户的基本信息
 */
@Entity
@Table(name = "users")
public class User {

    /** 用户ID，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名，唯一，用于登录 */
    @Column(nullable = false, unique = true, length = 30)
    private String username;

    /** 密码，BCrypt 加密存储 */
    @Column(nullable = false, length = 100)
    private String password;

    /** 用户昵称/显示名 */
    @Column(length = 30)
    private String nickname;

    /** 注册时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 绑定邮箱（可选） */
    @Column(length = 120)
    private String email;

    /** 邮箱是否已验证 */
    @Column(nullable = false)
    private boolean emailVerified = false;

    /** 邮箱验证令牌（一次性，验证后清空） */
    @Column(length = 64)
    private String emailVerificationToken;

    /** 邮箱验证令牌过期时间（24小时） */
    private LocalDateTime emailVerificationTokenExpiry;

    /** 密码重置令牌（一次性，5分钟内有效） */
    @Column(length = 64)
    private String passwordResetToken;

    /** 密码重置令牌过期时间 */
    private LocalDateTime passwordResetTokenExpiry;

    /** 头像路径（存储相对路径，通过 /uploads/avatars/ 访问） */
    @Column(length = 500)
    private String avatarPath;

    /** 微信收款码图片路径 */
    @Column(length = 500)
    private String wechatQrCode;

    /** 支付宝收款码图片路径 */
    @Column(length = 500)
    private String alipayQrCode;

    /** 连续登录失败次数 */
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    /** 账号锁定截止时间 */
    private LocalDateTime accountLockedUntil;

    // ===== Getter / Setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }

    public LocalDateTime getEmailVerificationTokenExpiry() { return emailVerificationTokenExpiry; }
    public void setEmailVerificationTokenExpiry(LocalDateTime emailVerificationTokenExpiry) { this.emailVerificationTokenExpiry = emailVerificationTokenExpiry; }

    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public LocalDateTime getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
    public void setPasswordResetTokenExpiry(LocalDateTime passwordResetTokenExpiry) { this.passwordResetTokenExpiry = passwordResetTokenExpiry; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public String getWechatQrCode() { return wechatQrCode; }
    public void setWechatQrCode(String wechatQrCode) { this.wechatQrCode = wechatQrCode; }

    public String getAlipayQrCode() { return alipayQrCode; }
    public void setAlipayQrCode(String alipayQrCode) { this.alipayQrCode = alipayQrCode; }
}
