package com.eldercare.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;

import java.time.LocalDateTime;

/**
 * 志愿者可服务时间请求。
 *
 * @param serviceItemId 服务项目 ID。
 * @param startTime     可服务开始时间。
 * @param endTime       可服务结束时间。
 */
public record VolunteerAvailableTimeDTO(
        // 服务项目 ID；表示该时间段志愿者愿意承接哪类服务。
        Long serviceItemId,
        // 可服务开始时间；必须早于 endTime。
        @NotNull(message = "不能为空") @Future(message = "必须是未来时间") LocalDateTime startTime,
        // 可服务结束时间；必须晚于 startTime。
        @NotNull(message = "不能为空") @Future(message = "必须是未来时间") LocalDateTime endTime) {
}
