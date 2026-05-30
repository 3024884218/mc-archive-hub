package com.mcarchive.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * 所有 Controller 抛出的异常统一在这里转换成 {"error": "..."} JSON 格式，
 * 不再需要在每个方法里 try-catch + return ResponseEntity。
 *
 * 设计原则：
 *   - 已知异常：返回客户端友好的错误信息
 *   - 未知异常：记录完整堆栈到日志，客户端只显示通用提示（不泄露内部路径/代码）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 参数校验失败 — @Valid 触发，返回所有字段错误 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> Map.of("field", e.getField(), "message", e.getDefaultMessage()))
            .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(Map.of(
            "error", "参数校验失败",
            "details", errors
        ));
    }

    /** 业务参数异常 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /** 未登录或权限不足 */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    /** 数据完整性冲突 — 唯一键重复、外键约束等 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause() != null
            ? ex.getMostSpecificCause().getMessage() : "";
        log.warn("数据完整性冲突: {}", cause);

        if (cause.contains("users.username") || cause.contains("username")) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名已被注册"));
        }
        if (cause.contains("users.email") || cause.contains("email")) {
            return ResponseEntity.badRequest().body(Map.of("error", "该邮箱已被其他账号绑定"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "操作冲突，请检查输入数据"));
    }

    /** 文件上传大小超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(Map.of("error", "文件大小超过限制（最大 500MB）"));
    }

    /** JSON 请求体解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "请求格式错误，请检查 JSON 数据"));
    }

    /** 兜底 — 未知异常（记录完整日志，客户端只显示通用消息） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("未处理的服务器异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "服务器内部错误，请稍后重试"));
    }
}
