package com.eldercare.common.log.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 日志 JSON 序列化和截断工具。
 */
public final class JsonLogUtil {
    /** 默认最大长度。 */
    public static final int DEFAULT_MAX_LENGTH = 4000;

    private JsonLogUtil() {
    }

    /**
     * 序列化、脱敏并截断对象。
     */
    public static String toSafeJson(Object value, ObjectMapper objectMapper, int maxLength) {
        // 空对象直接返回 null。
        if (value == null) {
            return null;
        }
        try {
            // 序列化成 JSON。
            String json = value instanceof String text ? text : objectMapper.writeValueAsString(value);
            // 脱敏后截断。
            return truncate(SensitiveDataUtil.mask(json), maxLength);
        } catch (JsonProcessingException exception) {
            // 序列化失败时使用对象字符串，避免影响主流程。
            return truncate(SensitiveDataUtil.mask(String.valueOf(value)), maxLength);
        }
    }

    /**
     * 截断文本。
     */
    public static String truncate(String text, int maxLength) {
        // 空文本直接返回。
        if (text == null) {
            return null;
        }
        // 长度未超限直接返回。
        if (text.length() <= maxLength) {
            return text;
        }
        // 截断并标记。
        return text.substring(0, Math.max(0, maxLength)) + "...[truncated]";
    }
}
