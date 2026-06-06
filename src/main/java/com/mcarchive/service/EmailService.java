package com.mcarchive.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 *
 * - 已配置 SMTP → 真实发送 HTML 邮件
 * - 未配置 SMTP → 输出到控制台日志（开发模式）
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String baseUrl;
    private final boolean emailConfigured;

    public boolean isEmailConfigured() { return emailConfigured; }

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String from,
                        @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.from = "MC Archive Hub <" + from + ">";
        this.baseUrl = baseUrl;

        // 检测是否配置了真实凭据（占位符或被注释掉）
        this.emailConfigured = from != null
            && !from.isBlank()
            && !from.contains("<")
            && !from.contains("@placeholder")
            && !from.contains("你的QQ");

        if (emailConfigured) {
            log.info("邮件服务已就绪，发件人: {}", from);
        } else {
            log.warn("邮件 SMTP 未配置真实凭据，邮件内容将输出到控制台日志");
        }
    }

    /**
     * 发送邮箱验证邮件
     */
    public void sendVerificationEmail(String to, String username, String token) {
        String subject = "MC Archive Hub — 邮箱验证";
        String htmlBody = buildVerificationEmail(username, token);

        send(to, subject, htmlBody);
    }

    /**
     * 发送密码重置邮件
     */
    public void sendPasswordResetEmail(String to, String username, String token) {
        String subject = "MC Archive Hub — 密码重置";
        String htmlBody = buildPasswordResetEmail(username, token);

        send(to, subject, htmlBody);
    }

    /** 发送设备验证码 */
    public void sendDeviceVerifyCode(String to, String username, String code) {
        String subject = "MC Archive Hub — 新设备登录验证";
        String htmlBody = "<div style='max-width:560px;margin:0 auto;font-family:-apple-system,sans-serif;padding:24px'>"
            + "<div style='background:#1a8a1a;padding:20px;border-radius:12px 12px 0 0'>"
            + "<h1 style='color:#fff;margin:0;font-size:20px'>新设备登录验证</h1></div>"
            + "<div style='background:#fff;border:1px solid #e5e7eb;border-top:none;padding:24px;border-radius:0 0 12px 12px'>"
            + "<p style='font-size:16px;margin:0 0 16px'>嗨 " + username + "，</p>"
            + "<p style='font-size:15px;color:#6b7280;margin:0 0 16px'>检测到你的账号在新设备上登录，验证码：</p>"
            + "<div style='background:#f0fdf4;border:2px dashed #1a8a1a;border-radius:12px;padding:20px;text-align:center;margin:16px 0'>"
            + "<span style='font-size:32px;font-weight:700;letter-spacing:8px;color:#1a8a1a'>" + code + "</span></div>"
            + "<p style='font-size:13px;color:#9ca3af;margin:16px 0 0'>10 分钟内有效。如非本人操作请忽略。</p>"
            + "</div></div>";
        send(to, subject, htmlBody);
    }

    // ===== 内部方法 =====

    private void send(String to, String subject, String htmlBody) {
        if (!emailConfigured) {
            // 开发模式：输出到控制台日志
            logToConsole(to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("邮件已发送: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("邮件发送失败，回退到控制台输出: to={}, error={}", to, e.getMessage());
            logToConsole(to, subject);
        }
    }

    private void logToConsole(String to, String subject) {
        log.info("");
        log.info("╔══════════════════════ 邮件（开发模式）══════════════════════╗");
        log.info("║  To:      {}", to);
        log.info("║  Subject: {}", subject);
        log.info("║  验证令牌请查看上方控制器返回的 user.emailVerificationToken");
        log.info("╚══════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private String buildVerificationEmail(String username, String token) {
        String verifyUrl = baseUrl + "/api/auth/verify-email?token=" + token;
        return """
                <div style="max-width:560px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,sans-serif;padding:24px">
                  <div style="background:#3a8f3a;padding:20px;border-radius:12px 12px 0 0">
                    <h1 style="color:#fff;margin:0;font-size:20px">MC Archive Hub</h1>
                    <p style="color:rgba(255,255,255,0.8);margin:4px 0 0;font-size:14px">邮箱验证</p>
                  </div>
                  <div style="background:#fff;border:1px solid #e5e7eb;border-top:none;padding:24px;border-radius:0 0 12px 12px">
                    <p style="font-size:16px;color:#1a1a2e;margin:0 0 16px">
                      嗨 <strong>%s</strong>，
                    </p>
                    <p style="font-size:15px;color:#6b7280;margin:0 0 24px;line-height:1.6">
                      请点击下方按钮验证你的邮箱地址（24小时内有效）：
                    </p>
                    <a href="%s"
                       style="display:inline-block;background:#3a8f3a;color:#fff;padding:12px 32px;border-radius:8px;text-decoration:none;font-size:15px;font-weight:600">
                      验证邮箱
                    </a>
                    <p style="font-size:13px;color:#9ca3af;margin:24px 0 0;line-height:1.5">
                      如果按钮无法点击，请复制以下链接到浏览器：<br>
                      <code style="word-break:break-all;color:#3a8f3a">%s</code>
                    </p>
                    <p style="font-size:13px;color:#9ca3af;margin:16px 0 0">
                      如果这不是你本人操作，请忽略此邮件。
                    </p>
                  </div>
                </div>
                """.formatted(username, verifyUrl, verifyUrl);
    }

    private String buildPasswordResetEmail(String username, String token) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        return """
                <div style="max-width:560px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,sans-serif;padding:24px">
                  <div style="background:#f59e0b;padding:20px;border-radius:12px 12px 0 0">
                    <h1 style="color:#fff;margin:0;font-size:20px">MC Archive Hub</h1>
                    <p style="color:rgba(255,255,255,0.8);margin:4px 0 0;font-size:14px">密码重置</p>
                  </div>
                  <div style="background:#fff;border:1px solid #e5e7eb;border-top:none;padding:24px;border-radius:0 0 12px 12px">
                    <p style="font-size:16px;color:#1a1a2e;margin:0 0 16px">
                      嗨 <strong>%s</strong>，
                    </p>
                    <p style="font-size:15px;color:#6b7280;margin:0 0 24px;line-height:1.6">
                      你请求了密码重置。点击下方按钮设置新密码（<strong>5分钟</strong>内有效）：
                    </p>
                    <a href="%s"
                       style="display:inline-block;background:#f59e0b;color:#1a1a2e;padding:12px 32px;border-radius:8px;text-decoration:none;font-size:15px;font-weight:600">
                      重置密码
                    </a>
                    <p style="font-size:13px;color:#9ca3af;margin:24px 0 0;line-height:1.5">
                      如果按钮无法点击，请复制以下链接到浏览器：<br>
                      <code style="word-break:break-all;color:#f59e0b">%s</code>
                    </p>
                    <p style="font-size:13px;color:#9ca3af;margin:16px 0 0">
                      如果这不是你本人操作，请忽略此邮件。
                    </p>
                  </div>
                </div>
                """.formatted(username, resetUrl, resetUrl);
    }
}
