package com.eldercare.order.producer;

import com.eldercare.common.enums.MqEnum;
import com.eldercare.order.message.OrderTimeoutMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息生产者。
 * 说明：使用 RabbitMQ 消息 TTL + 死信队列实现公共池订单 30 分钟自动取消。
 */
@Component
public class OrderTimeoutProducer {
    /** RabbitMQ 模板。 */
    private final RabbitTemplate rabbitTemplate;

    public OrderTimeoutProducer(RabbitTemplate rabbitTemplate) {
        // 保存 RabbitTemplate。
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送订单超时延迟消息。
     *
     * @param message      超时消息。
     * @param delayMillis  延迟毫秒数。
     */
    public void sendTimeoutMessage(OrderTimeoutMessage message, long delayMillis) {
        // RabbitMQ expiration 必须是字符串毫秒值，最小给 1ms，避免 0 导致语义不清晰。
        String expiration = String.valueOf(Math.max(1L, delayMillis));
        // 投递到延迟交换机，消息在延迟队列中过期后进入死信队列。
        rabbitTemplate.convertAndSend(
                MqEnum.ORDER_TIMEOUT_DELAY_EXCHANGE.code(),
                MqEnum.ORDER_TIMEOUT_DELAY_ROUTING_KEY.code(),
                message,
                msg -> {
                    // 设置消息级 TTL。
                    msg.getMessageProperties().setExpiration(expiration);
                    // 返回修改后的消息。
                    return msg;
                });
    }
}
