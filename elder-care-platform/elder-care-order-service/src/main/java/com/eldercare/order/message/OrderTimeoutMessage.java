package com.eldercare.order.message;

import java.time.LocalDateTime;

/**
 * 订单超时死信消息。
 *
 * @param orderId     订单 ID。
 * @param communityId 社区 ID。
 * @param grabDeadline 抢单截止时间。
 */
public record OrderTimeoutMessage(Long orderId, Long communityId, LocalDateTime grabDeadline) {
}
