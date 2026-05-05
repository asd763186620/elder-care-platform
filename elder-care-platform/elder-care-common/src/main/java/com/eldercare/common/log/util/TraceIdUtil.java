package com.eldercare.common.log.util;

import com.eldercare.common.enums.HeaderEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * TraceId 工具类。
 */
public final class TraceIdUtil {
    /** MDC 中保存 traceId 的 key。 */
    public static final String MDC_TRACE_ID = "traceId";

    private TraceIdUtil() {
    }

    /**
     * 生成新的 traceId。
     */
    public static String newTraceId() {
        // 使用 UUID 去掉横线，便于日志检索。
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从当前 Servlet 请求或 MDC 中获取 traceId。
     */
    public static String currentTraceId() {
        // 优先从 MDC 读取。
        String traceId = MDC.get(MDC_TRACE_ID);
        // MDC 中存在时直接返回。
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }
        // 尝试从当前请求头读取。
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 请求上下文不存在时生成新 traceId。
        if (attributes == null) {
            return newTraceId();
        }
        // 读取请求头。
        HttpServletRequest request = attributes.getRequest();
        // 返回请求头 traceId 或新 traceId。
        return StringUtils.hasText(request.getHeader(HeaderEnum.TRACE_ID.code())) ? request.getHeader(HeaderEnum.TRACE_ID.code()) : newTraceId();
    }
}
