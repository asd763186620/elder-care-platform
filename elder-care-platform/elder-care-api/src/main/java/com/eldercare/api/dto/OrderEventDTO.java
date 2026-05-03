package com.eldercare.api.dto;

import java.time.LocalDateTime;

/**
 * 订单事件消息 DTO。
 *
 * @param messageId   消息唯一 ID。
 * @param eventType   事件类型。
 * @param communityId 社区 ID。
 * @param orderId     订单 ID。
 * @param orderNo     订单号。
 * @param elderUserId 老人用户 ID。
 * @param operatorId  操作人用户 ID。
 * @param content     通知内容。
 * @param eventTime   事件时间。
 */
public record OrderEventDTO(String messageId,
                            String eventType,
                            Long communityId,
                            Long orderId,
                            String orderNo,
                            Long elderUserId,
                            Long operatorId,
                            String content,
                            LocalDateTime eventTime) {
}
