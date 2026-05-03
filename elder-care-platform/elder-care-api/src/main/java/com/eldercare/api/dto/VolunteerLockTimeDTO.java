package com.eldercare.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 志愿者时间锁请求。
 *
 * @param communityId     社区 ID。
 * @param volunteerUserId 志愿者用户 ID。
 * @param orderId         订单 ID。
 * @param serviceItemId   服务项目 ID。
 * @param startTime       锁定开始时间。
 * @param endTime         锁定结束时间。
 */
public record VolunteerLockTimeDTO(
        // 社区 ID；释放和锁定都必须带社区条件，防止跨社区误操作。
        @NotNull(message = "不能为空") Long communityId,
        // 志愿者用户 ID；表示要锁定哪位志愿者的服务时间。
        @NotNull(message = "不能为空") Long volunteerUserId,
        // 订单 ID；表示该时间锁由哪笔订单占用。
        @NotNull(message = "不能为空") Long orderId,
        // 服务项目 ID；锁定前二次校验志愿者可服务时间时需要使用。
        Long serviceItemId,
        // 锁定开始时间；和 endTime 一起生成 timeSlotKey。
        @NotNull(message = "不能为空") LocalDateTime startTime,
        // 锁定结束时间；和 startTime 一起判断唯一时间段。
        @NotNull(message = "不能为空") LocalDateTime endTime) {
}
