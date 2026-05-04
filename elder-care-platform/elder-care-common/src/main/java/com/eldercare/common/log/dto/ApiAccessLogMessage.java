package com.eldercare.common.log.dto;

import java.time.LocalDateTime;

/**
 * Gateway 接口访问日志消息。
 */
public record ApiAccessLogMessage(
        // 请求链路 ID。
        String traceId,
        // 当前登录用户 ID。
        Long userId,
        // 当前登录身份。
        String roleType,
        // 当前社区 ID。
        Long communityId,
        // HTTP 请求方法。
        String requestMethod,
        // 请求路径。
        String requestUri,
        // 查询参数。
        String queryParams,
        // 客户端 IP。
        String clientIp,
        // User-Agent。
        String userAgent,
        // 响应状态码。
        Integer statusCode,
        // 接口耗时，单位毫秒。
        Long costTime,
        // 是否慢接口。
        Boolean slowFlag,
        // 异常信息。
        String errorMessage,
        // 请求时间。
        LocalDateTime requestTime
) {
}
