package com.mcarchive.security;

import com.mcarchive.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 获取当前登录用户的工具类
 *
 * 替代 AuthController.getCurrentUserDetails() 静态方法，
 * 提升可测试性（可 mock），降低 Controller 层耦合。
 */
@Component
public class CurrentUser {

    /**
     * 获取当前登录的 CustomUserDetails，未登录返回 null
     */
    public CustomUserDetails getDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) auth.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前登录用户（必须登录，否则抛异常）
     * @throws SecurityException 未登录时
     */
    public User require() {
        CustomUserDetails details = getDetails();
        if (details == null) {
            throw new SecurityException("请先登录");
        }
        return details.getUser();
    }

    /**
     * 获取当前登录用户，未登录返回 null
     */
    public User get() {
        CustomUserDetails details = getDetails();
        return details != null ? details.getUser() : null;
    }
}
