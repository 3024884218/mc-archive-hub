package com.mcarchive.controller;

import com.mcarchive.model.User;
import com.mcarchive.security.CurrentUser;
import com.mcarchive.security.CustomUserDetails;
import com.mcarchive.security.RateLimiter;
import com.mcarchive.service.EmailService;
import com.mcarchive.service.FileStorageService;
import com.mcarchive.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 认证控制器
 * 注册 → 调用 UserService → 构造 CustomUserDetails → 注入 SecurityContext
 * 登录 → 同上
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    private final CurrentUser currentUser;
    private final RateLimiter rateLimiter;

    public AuthController(UserService userService,
                          EmailService emailService,
                          FileStorageService fileStorageService,
                          CurrentUser currentUser,
                          RateLimiter rateLimiter) {
        this.userService = userService;
        this.emailService = emailService;
        this.fileStorageService = fileStorageService;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 用户注册 — 创建账号但不自动登录，必须验证邮箱后才能登录
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body,
                                       HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");

        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }
        if (username.length() > 30) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名最长30个字符"));
        }
        password = password.trim();
        if (password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码至少8位"));
        }

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入邮箱"));
        }
        email = email.trim().toLowerCase();
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "邮箱格式不正确"));
        }

        // 速率限制
        if (!rateLimiter.allowRegister(getClientIp(request))) {
            return ResponseEntity.status(429).body(Map.of("error", "操作过于频繁，请稍后再试"));
        }

        User user = userService.register(username.trim(), password, email);

        // 发送验证邮件
        try {
            emailService.sendVerificationEmail(email, user.getUsername(),
                user.getEmailVerificationToken());
        } catch (Exception e) {
            log.warn("验证邮件发送失败: {}", e.getMessage());
        }

        // 不自动登录，引导用户去邮箱验证
        return ResponseEntity.ok(Map.of(
            "message", "注册成功！验证邮件已发送至 " + email + "，请查收后点击验证链接完成激活",
            "needVerify", true
        ));
    }

    /**
     * 用户登录 — 邮箱未验证则拒绝登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                    HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }

        // 速率限制
        if (!rateLimiter.allowLogin(getClientIp(request))) {
            return ResponseEntity.status(429).body(Map.of("error", "操作过于频繁，请稍后再试"));
        }

        User user;
        try {
            user = userService.authenticate(username.trim(), password != null ? password.trim() : null);
        } catch (IllegalArgumentException e) {
            // 账号被锁定等情况
            return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
        }

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }

        // 邮箱未验证则拒绝
        if (user.getEmail() != null && !user.isEmailVerified()) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "邮箱尚未验证，请查收验证邮件并点击链接激活账号",
                "needVerify", true,
                "email", user.getEmail()
            ));
        }

        autoLogin(request, user);
        return ResponseEntity.ok(Map.of("message", "登录成功", "user", userToMap(user)));
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        CustomUserDetails details = currentUser.getDetails();
        if (details == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        return ResponseEntity.ok(userToMap(details.getUser()));
    }

    // ===== 内部方法 =====

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 自动登录：将用户注入 SecurityContext 并持久化到 Session
     * 调用 changeSessionId() 防止会话固定攻击
     */
    private void autoLogin(HttpServletRequest request, User user) {
        // 防止会话固定攻击：登录成功后更换 Session ID
        request.changeSessionId();

        CustomUserDetails details = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        HttpSession session = request.getSession(true);
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        );
    }

    private Map<String, Object> userToMap(User user) {
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "email", user.getEmail() != null ? user.getEmail() : "",
            "emailVerified", user.isEmailVerified(),
            "avatarUrl", user.getAvatarPath() != null
                ? "/uploads/" + user.getAvatarPath() : "",
            "wechatQrCodeUrl", user.getWechatQrCode() != null
                ? "/uploads/" + user.getWechatQrCode() : null,
            "alipayQrCodeUrl", user.getAlipayQrCode() != null
                ? "/uploads/" + user.getAlipayQrCode() : null,
            "contactEmail", user.getContactEmail() != null ? user.getContactEmail() : null
        );
    }

    // ===== 邮箱绑定 =====

    /**
     * 绑定邮箱 — 发送验证邮件
     */
    @PostMapping("/bind-email")
    public ResponseEntity<?> bindEmail(@RequestBody Map<String, String> body) {
        User user = currentUser.require();
        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "邮箱不能为空"));
        }
        email = email.trim().toLowerCase();

        // 基本格式校验
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "邮箱格式不正确"));
        }

        try {
            userService.bindEmail(user, email);
            return ResponseEntity.ok(Map.of(
                "message", "验证邮件已发送至 " + email + "，请查收",
                "user", userToMap(user)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 验证邮箱 — 用户点击邮件中的链接，成功后重定向到首页
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的验证链接"));
        }

        boolean ok = userService.verifyEmail(token.trim());
        if (ok) {
            // 验证成功，重定向到首页并显示成功提示
            return ResponseEntity.status(302)
                .header("Location", "/?verified=ok")
                .build();
        } else {
            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body("""
                    <html><head><meta charset="UTF-8"><title>验证失败</title></head>
                    <body style="font-family:sans-serif;text-align:center;padding:80px 20px">
                      <h2 style="color:#ef4444">验证链接无效或已过期</h2>
                      <p>请返回网站重新发送验证邮件。</p>
                    </body></html>""");
        }
    }

    // ===== 个人资料修改 =====

    /**
     * 修改昵称
     */
    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        User user = currentUser.require();
        String nickname = body.get("nickname");

        if (nickname == null || nickname.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "昵称不能为空"));
        }
        if (nickname.trim().length() > 30) {
            return ResponseEntity.badRequest().body(Map.of("error", "昵称最多30个字符"));
        }

        userService.updateProfile(user, nickname.trim());
        return ResponseEntity.ok(Map.of(
            "message", "资料已更新",
            "user", userToMap(user)
        ));
    }

    /**
     * 修改密码（需要旧密码验证）
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        User user = currentUser.require();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入旧密码和新密码"));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "新密码至少8位"));
        }

        boolean ok = userService.changePassword(user, oldPassword, newPassword);
        if (!ok) {
            return ResponseEntity.status(401).body(Map.of("error", "旧密码不正确"));
        }

        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }

    /**
     * 上传头像
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        User user = currentUser.require();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择头像图片"));
        }

        try {
            String path = fileStorageService.storeAvatar(user.getId(), file);
            user.setAvatarPath(path);
            userService.save(user);
            return ResponseEntity.ok(Map.of(
                "message", "头像已更新",
                "user", userToMap(user)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "头像上传失败"));
        }
    }

    /**
     * 上传收款码（微信/支付宝）
     */
    @PostMapping("/upload-qrcode")
    public ResponseEntity<?> uploadQrCode(@RequestParam("type") String type,
                                           @RequestParam("file") MultipartFile file) {
        User user = currentUser.require();

        if (!"wx".equals(type) && !"ali".equals(type)) {
            return ResponseEntity.badRequest().body(Map.of("error", "类型必须为 wx 或 ali"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择二维码图片"));
        }

        try {
            String path = fileStorageService.storeQrCode(user.getId(), type, file);
            if ("wx".equals(type)) {
                user.setWechatQrCode(path);
            } else {
                user.setAlipayQrCode(path);
            }
            userService.save(user);
            return ResponseEntity.ok(Map.of(
                "message", "收款码已更新",
                "user", userToMap(user)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("收款码上传失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "收款码上传失败"));
        }
    }

    /**
     * 更新联系邮箱
     */
    @PostMapping("/update-contact-email")
    public ResponseEntity<?> updateContactEmail(@RequestBody Map<String, String> body) {
        User user = currentUser.require();
        String email = body.get("email");

        if (email != null && !email.trim().isEmpty()) {
            email = email.trim().toLowerCase();
            if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "邮箱格式不正确"));
            }
            user.setContactEmail(email);
        } else {
            user.setContactEmail(null);
        }
        userService.save(user);
        return ResponseEntity.ok(Map.of(
            "message", email != null ? "联系邮箱已更新" : "联系邮箱已清除",
            "user", userToMap(user)
        ));
    }

    // ===== 密码重置 =====

    /**
     * 请求密码重置 — 向已绑定且已验证的邮箱发送重置链接
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body,
                                             HttpServletRequest request) {
        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入邮箱"));
        }

        // 速率限制
        if (!rateLimiter.allowForgotPassword(getClientIp(request))) {
            return ResponseEntity.status(429).body(Map.of("error", "操作过于频繁，请稍后再试"));
        }

        try {
            userService.requestPasswordReset(email.trim().toLowerCase());
            return ResponseEntity.ok(Map.of("message", "如果该邮箱已绑定且验证，重置链接已发送"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 通过令牌重置密码
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("password");

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的重置链接"));
        }
        if (newPassword == null || newPassword.trim().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "新密码至少 8 位"));
        }
        newPassword = newPassword.trim();

        boolean ok = userService.resetPassword(token.trim(), newPassword);
        if (ok) {
            return ResponseEntity.ok(Map.of("message", "密码重置成功，请使用新密码登录"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "重置链接无效或已过期（5分钟有效）"));
        }
    }
}
