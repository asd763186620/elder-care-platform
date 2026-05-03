package com.eldercare.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 查询可用志愿者请求 DTO。
 *
 * @param serviceItemId 服务项目 ID。
 * @param startTime     服务开始时间。
 * @param endTime       服务结束时间。
 */
public record AvailableVolunteerQueryDTO(
        // 服务项目 ID 用于匹配志愿者可服务范围。
        @NotNull(message = "不能为空") Long serviceItemId,
        // 开始时间用于检查志愿者时间是否空闲。
        @NotNull(message = "不能为空") LocalDateTime startTime,
        // 结束时间用于检查志愿者时间是否空闲。
        @NotNull(message = "不能为空") LocalDateTime endTime
) {
}
