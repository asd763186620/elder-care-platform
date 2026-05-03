package com.eldercare.api.vo;

import java.time.LocalDateTime;

/**
 * 预约单 VO。
 *
 * @param id                       订单 ID。
 * @param communityId              社区 ID。
 * @param orderNo                  订单号。
 * @param elderUserId              被服务老人用户 ID。
 * @param creatorUserId            创建订单的用户 ID。
 * @param serviceItemId            服务项目 ID。
 * @param serviceAddress           服务地址。
 * @param serviceStartTime         服务开始时间。
 * @param serviceEndTime           服务结束时间。
 * @param assignMode               派单模式。
 * @param specifiedVolunteerUserId 指定志愿者用户 ID。
 * @param assignedVolunteerUserId  实际接单志愿者用户 ID。
 * @param orderStatus              订单状态。
 * @param remark                   订单备注。
 */
public record OrderVO(
        // 订单 ID；前端后续取消、抢单、完成都通过它定位订单。
        Long id,
        // 社区 ID；用于前端确认订单所属社区。
        Long communityId,
        // 订单号；适合展示给用户和客服查询。
        String orderNo,
        // 被服务老人用户 ID。
        Long elderUserId,
        // 创建订单的用户 ID，可能是老人也可能是亲情号。
        Long creatorUserId,
        // 服务项目 ID。
        Long serviceItemId,
        // 服务地址。
        String serviceAddress,
        // 服务开始时间。
        LocalDateTime serviceStartTime,
        // 服务结束时间。
        LocalDateTime serviceEndTime,
        // 派单模式：ASSIGNED 或 PUBLIC。
        String assignMode,
        // 下单时指定的志愿者 ID，公共订单池模式为空。
        Long specifiedVolunteerUserId,
        // 实际接单志愿者 ID，公共池抢单成功后写入。
        Long assignedVolunteerUserId,
        // 当前订单状态。
        String orderStatus,
        // 订单备注。
        String remark) {
}
