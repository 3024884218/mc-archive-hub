package com.mcarchive.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Spring Security 安全配置
 * 使用 Session 认证，API 返回 JSON
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 保护：Cookie 存储 token，SPA 从 Cookie 读取后放入 X-XSRF-TOKEN Header
            // 登录/注册/验证/上传等接口豁免：session 认证 + requireAuth() 已足够
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**")
            )

            // 权限配置
            .authorizeHttpRequests(auth -> auth
                // 公开访问：静态资源 + 认证接口 + 存档浏览
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/uploads/**",
                                 "/api/auth/register", "/api/auth/login",
                                 "/api/auth/verify-email", "/api/auth/verify-device",
                                 "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                // 存档读取公开（浏览不需要登录）；写入操作由 Controller 内 requireAuth() 保护
                .requestMatchers("GET", "/api/archives/**").permitAll()
                // 其他 API 需要登录
                .anyRequest().authenticated()
            )

            // Session 认证：同一账号最多 1 个会话，新登录踢掉旧会话
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )

            // 登出配置
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(jsonLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    /**
     * 登出成功时返回 JSON 而非重定向
     */
    private LogoutSuccessHandler jsonLogoutSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.core.Authentication authentication) -> {
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"已退出登录\"}");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
