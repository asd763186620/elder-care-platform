package com.eldercare.common.log.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 日志脱敏工具。
 */
public final class SensitiveDataUtil {
    /** 敏感字段关键字。 */
    private static final Set<String> SENSITIVE_KEYS = Set.of("password", "token", "authorization", "refreshToken", "secret", "appSecret");

    private SensitiveDataUtil() {
    }

    /**
     * 对 JSON 文本中的敏感字段做脱敏。
     */
    public static String mask(String text) {
        // 空文本直接返回。
        if (text == null || text.isBlank()) {
            return text;
        }
        // 逐个敏感字段替换 JSON 值。
        String result = text;
        // 遍历敏感字段。
        for (String key : SENSITIVE_KEYS) {
            // 将 "key":"value" 替换成 "key":"******"。
            result = Pattern.compile("(\"" + key + "\"\\s*:\\s*\")([^\"]*)(\")", Pattern.CASE_INSENSITIVE)
                    .matcher(result)
                    .replaceAll("$1******$3");
        }
        // 返回脱敏结果。
        return result;
    }
}
