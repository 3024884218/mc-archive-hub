package com.mcarchive.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易内存速率限制器
 * 按 IP 限制关键认证接口的调用频率，防止暴力破解
 */
@Component
public class RateLimiter {

    private static final int LOGIN_MAX = 10;
    private static final int REGISTER_MAX = 3;
    private static final int FORGOT_PASSWORD_MAX = 3;
    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    /**
     * @return true 表示允许请求，false 表示超限
     */
    public boolean allowLogin(String ip) {
        return allow(ip, LOGIN_MAX);
    }

    public boolean allowRegister(String ip) {
        return allow(ip, REGISTER_MAX);
    }

    public boolean allowForgotPassword(String ip) {
        return allow(ip, FORGOT_PASSWORD_MAX);
    }

    private boolean allow(String ip, int max) {
        Instant now = Instant.now();
        Window w = buckets.compute(ip, (k, v) -> {
            if (v == null || v.timestamp.plusSeconds(WINDOW_SECONDS).isBefore(now)) {
                return new Window(now);
            }
            v.count++;
            return v;
        });
        return w.count <= max;
    }

    private static class Window {
        final Instant timestamp;
        int count;

        Window(Instant timestamp) {
            this.timestamp = timestamp;
            this.count = 1;
        }
    }
}
