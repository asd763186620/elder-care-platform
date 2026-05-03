package com.eldercare.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 校验志愿者可用请求。
 *
 * @param communityId     社区 ID。
 * @param volunteerUserId 志愿者用户 ID。
 * @param serviceItemId   服务项目 ID。
 * @param startTime       服务开始时间。
 * @param endTime         服务结束时间。
 */
public record VolunteerCheckAvailableDTO(
        // 社区 ID；用于保证志愿者和订单在同一社区。
        @NotNull(message = "不能为空") Long communityId,
        // 志愿者用户 ID；对应 user_db.user_account.id。
        @NotNull(message = "不能为空") Long volunteerUserId,
        // 服务项目 ID；用于匹配志愿者配置的可服务项目。
        Long serviceItemId,
        // 订单预约开始时间；用于判断是否落在志愿者可服务时间内。
        @NotNull(message = "不能为空") LocalDateTime startTime,
        // 订单预约结束时间；用于判断是否和已有时间锁冲突。
        @NotNull(message = "不能为空") LocalDateTime endTime) {
}
