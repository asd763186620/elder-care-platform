package com.eldercare.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 订单 Outbox 本地消息实体。
 */
@TableName("order_event_outbox")
public class OrderEventOutbox {
    /** 主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 事件唯一 ID。 */
    private String eventId;
    /** 订单 ID。 */
    private Long orderId;
    /** 事件类型。 */
    private String eventType;
    /** 交换机名称。 */
    private String exchangeName;
    /** 路由键。 */
    private String routingKey;
    /** JSON 消息体。 */
    private String payload;
    /** 状态：INIT、SENT、FAILED。 */
    private String status;
    /** 重试次数。 */
    private Integer retryCount;
    /** 下次重试时间。 */
    private LocalDateTime nextRetryTime;
    /** 最近一次错误。 */
    private String lastError;

    /** 创建本地消息。 */
    public static OrderEventOutbox init(String eventId, Long orderId, String eventType, String exchangeName, String routingKey, String payload) {
        // 构造 Outbox 行。
        OrderEventOutbox row = new OrderEventOutbox();
        // 写入事件 ID。
        row.eventId = eventId;
        // 写入订单 ID。
        row.orderId = orderId;
        // 写入事件类型。
        row.eventType = eventType;
        // 写入交换机。
        row.exchangeName = exchangeName;
        // 写入路由键。
        row.routingKey = routingKey;
        // 写入 JSON 消息体。
        row.payload = payload;
        // 初始状态待发送。
        row.status = "INIT";
        // 初始重试次数为 0。
        row.retryCount = 0;
        // 立即可发送。
        row.nextRetryTime = LocalDateTime.now();
        // 返回本地消息。
        return row;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }
    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
