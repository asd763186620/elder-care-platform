package com.eldercare.notify.consumer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.eldercare.api.dto.OrderEventDTO;
import com.eldercare.common.constant.MqConstants;
import com.eldercare.notify.entity.NotifyRecord;
import com.eldercare.notify.mapper.NotifyRecordMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知消息消费者，第一版先保留队列监听骨架。
 */
@Component
public class NotifyMessageConsumer {
    /**
     * 通知记录 Mapper。
     */
    private final NotifyRecordMapper notifyRecordMapper;

    public NotifyMessageConsumer(NotifyRecordMapper notifyRecordMapper) {
        // 保存通知记录 Mapper。
        this.notifyRecordMapper = notifyRecordMapper;
    }

    /**
     * 消费订单事件并写入通知记录。
     *
     * @param event 订单事件。
     */
    @RabbitListener(queues = MqConstants.ORDER_NOTIFY_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    @Transactional(rollbackFor = Exception.class)
    public void consume(OrderEventDTO event) {
        // 先按 MQ 消息 ID 查询是否已消费，避免重复通知。
        Long exists = notifyRecordMapper.selectCount(new QueryWrapper<NotifyRecord>().eq("mq_message_id", event.messageId()));
        // 已存在通知记录说明消息重复投递或重复消费。
        if (exists != null && exists > 0) {
            // 直接返回，让 RabbitMQ ack 当前消息。
            return;
        }
        // 构造通知记录。
        NotifyRecord record = new NotifyRecord();
        // 写入社区 ID。
        record.communityId = event.communityId();
        // 写入业务类型，这里直接使用订单事件类型。
        record.businessType = event.eventType();
        // 写入业务 ID，也就是订单 ID。
        record.businessId = event.orderId();
        // 第一版先通知老人用户。
        record.receiverUserId = event.elderUserId();
        // 第一版模拟站内通知。
        record.notifyChannel = "IN_APP";
        // 写入通知标题。
        record.notifyTitle = "养老服务预约通知";
        // 写入通知内容。
        record.notifyContent = event.content();
        // 第一版模拟发送成功，直接置为 2。
        record.notifyStatus = 2;
        // 初始重试次数为 0。
        record.retryCount = 0;
        // 记录 MQ 消息 ID，后续消费幂等依赖该字段唯一索引。
        record.mqMessageId = event.messageId();
        // 设置未删除。
        record.deleted = 0;
        // 插入通知记录。
        notifyRecordMapper.insert(record);
        // 模拟发送通知，后续可替换为短信或小程序订阅消息。
        System.out.println("mock send notify: " + event);
    }
}
