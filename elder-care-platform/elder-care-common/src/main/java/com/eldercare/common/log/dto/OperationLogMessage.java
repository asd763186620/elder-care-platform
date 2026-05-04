package com.eldercare.common.log.dto;

import java.time.LocalDateTime;

/**
 * 业务操作审计日志消息。
 */
public record OperationLogMessage(
        // 请求链路 ID。
        String traceId,
        // 当前登录用户 ID。
        Long userId,
        // 当前登录身份。
        String roleType,
        // 当前社区 ID。
        Long communityId,
        // 业务模块。
        String module,
        // 操作类型。
        String operationType,
        // 操作描述。
        String description,
        // 业务 ID。
        String bizId,
        // 类名。
        String className,
        // 方法名。
        String methodName,
        // 请求参数 JSON。
        String requestParams,
        // 返回结果 JSON。
        String responseResult,
        // 是否成功。
        Boolean successFlag,
        // 异常信息。
        String errorMessage,
        // 方法耗时。
        Long costTime,
        // 操作时间。
        LocalDateTime operationTime
) {
}
