package com.eldercare.order.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.eldercare.api.dto.OrderEventDTO;
import com.eldercare.order.entity.OrderEventOutbox;
import com.eldercare.order.mapper.OrderEventOutboxMapper;
import com.eldercare.order.producer.OrderEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 消息发送任务。
 * 说明：订单事务只写本地消息表，本任务异步扫描并发送 RabbitMQ，保证业务事务和消息最终一致。
 */
@Component
public class OrderOutboxPublishJob {
    /** Outbox Mapper。 */
    private final OrderEventOutboxMapper outboxMapper;
    /** MQ 生产者。 */
    private final OrderEventProducer producer;
    /** JSON 解析器。 */
    private final ObjectMapper objectMapper;

    public OrderOutboxPublishJob(OrderEventOutboxMapper outboxMapper, OrderEventProducer producer, ObjectMapper objectMapper) {
        // 保存 Outbox Mapper。
        this.outboxMapper = outboxMapper;
        // 保存 MQ 生产者。
        this.producer = producer;
        // 保存 JSON 解析器。
        this.objectMapper = objectMapper;
    }

    /**
     * 每 5 秒扫描一次待发送消息。
     */
    @Scheduled(fixedDelay = 5000)
    public void publishPending() {
        // 查询 INIT 或 FAILED 且到达下次重试时间的消息。
        List<OrderEventOutbox> rows = outboxMapper.selectList(new QueryWrapper<OrderEventOutbox>()
                .in("status", List.of("INIT", "FAILED"))
                .le("next_retry_time", LocalDateTime.now())
                .orderByAsc("id")
                .last("limit 50"));
        // 逐条发送，避免单条异常影响整批。
        for (OrderEventOutbox row : rows) {
            // 发送单条消息。
            publishOne(row);
        }
    }

    /**
     * 发送单条 Outbox 消息。
     */
    private void publishOne(OrderEventOutbox row) {
        try {
            // 将 payload 解析成订单事件 DTO。
            OrderEventDTO event = objectMapper.readValue(row.getPayload(), OrderEventDTO.class);
            // 发送到 RabbitMQ。
            producer.send(row.getRoutingKey(), event);
            // 发送成功后标记 SENT。
            outboxMapper.update(null, new UpdateWrapper<OrderEventOutbox>()
                    .set("status", "SENT")
                    .set("last_error", null)
                    .eq("id", row.getId())
                    .ne("status", "SENT"));
        } catch (Exception exception) {
            // 失败后递增重试次数。
            int retryCount = row.getRetryCount() == null ? 1 : row.getRetryCount() + 1;
            // 简单退避：最多延迟 60 秒。
            LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(Math.min(60, retryCount * 5L));
            // 截断错误文本，避免超过数据库字段长度。
            String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            if (error.length() > 500) {
                // last_error 字段长度有限，只保存前 500 字符。
                error = error.substring(0, 500);
            }
            // 标记失败并保存下次重试时间。
            outboxMapper.update(null, new UpdateWrapper<OrderEventOutbox>()
                    .set("status", "FAILED")
                    .set("retry_count", retryCount)
                    .set("next_retry_time", nextRetryTime)
                    .set("last_error", error)
                    .eq("id", row.getId())
                    .ne("status", "SENT"));
        }
    }
}
