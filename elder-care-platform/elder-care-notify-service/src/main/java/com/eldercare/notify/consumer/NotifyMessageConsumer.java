package com.eldercare.notify.consumer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.eldercare.api.dto.OrderEventDTO;
import com.eldercare.common.enums.MqEnum;
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
    @RabbitListener(queues = "#{T(com.eldercare.common.enums.MqEnum).ORDER_NOTIFY_QUEUE.code()}", containerFactory = "rabbitListenerContainerFactory")
    @Transactional(rollbackFor = Exception.class)
    public void consume(OrderEventDTO event) {
        // 先按 MQ 消息 ID 查询是否已消费，避免重复通知。
        Long exists = notifyRecordMapper.selectCount(new QueryWrapper<NotifyRecord>().eq("mq_message_id", event.messageId()));
        // 已存在通知记录说明消息重复投递或重复消费。
        if (exists != null && exists > 0) {
            // 直接返回，让 RabbitMQ ack 当前消息。
            return;
        }
        // 通过实体工厂构造通知记录，让消费端只负责编排消费流程。
        NotifyRecord record = NotifyRecord.orderSuccess(event.communityId(), event.eventType(), event.orderId(), event.elderUserId(), event.content(), event.messageId());
        // 插入通知记录。
        notifyRecordMapper.insert(record);
        // 模拟发送通知，后续可替换为短信或小程序订阅消息。
        System.out.println("mock send notify: " + event);
    }
}
