package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 创建预约单请求。
 *
 * @param elderUserId              被服务老人用户 ID。
 * @param serviceItemId            服务项目 ID。
 * @param serviceAddress           服务地址。
 * @param serviceStartTime         预约开始时间。
 * @param serviceEndTime           预约结束时间。
 * @param assignMode               派单模式：ASSIGNED 指定志愿者，PUBLIC 公共订单池。
 * @param specifiedVolunteerUserId 指定志愿者用户 ID，仅 ASSIGNED 模式必填。
 * @param remark                   订单备注。
 */
public record OrderCreateDTO(
        // 被服务老人用户 ID；老人自己下单时通常等于当前登录用户 ID。
        @NotNull(message = "不能为空") Long elderUserId,
        // 服务项目 ID；order-service 会调用 community-service 校验它属于当前社区。
        @NotNull(message = "不能为空") Long serviceItemId,
        // 服务地址；第一版直接保存文本地址。
        @NotBlank(message = "不能为空") @Size(max = 255, message = "不能超过255个字符") String serviceAddress,
        // 预约服务开始时间；必须早于 serviceEndTime。
        @NotNull(message = "不能为空") @Future(message = "必须是未来时间") LocalDateTime serviceStartTime,
        // 预约服务结束时间；必须晚于 serviceStartTime。
        @NotNull(message = "不能为空") @Future(message = "必须是未来时间") LocalDateTime serviceEndTime,
        // 派单模式；ASSIGNED 表示指定志愿者，PUBLIC 表示进入公共订单池。
        @NotBlank(message = "不能为空") @Size(max = 32, message = "不能超过32个字符") String assignMode,
        // 指定志愿者用户 ID；PUBLIC 模式可以为空。
        Long specifiedVolunteerUserId,
        // 下单备注；用于补充老人需求、门牌号或注意事项。
        @Size(max = 512, message = "不能超过512个字符") String remark) {
}
