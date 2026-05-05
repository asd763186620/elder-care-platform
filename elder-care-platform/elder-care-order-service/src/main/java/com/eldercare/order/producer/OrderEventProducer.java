package com.eldercare.order.producer;

import com.eldercare.api.dto.OrderEventDTO;
import com.eldercare.common.enums.MqEnum;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单事件生产者。
 */
@Component
public class OrderEventProducer {
    /**
     * Spring AMQP 消息发送模板。
     */
    private final RabbitTemplate rabbitTemplate;

    public OrderEventProducer(RabbitTemplate rabbitTemplate) {
        // 保存 RabbitTemplate。
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送订单事件。
     *
     * @param routingKey 路由键。
     * @param event      订单事件 DTO。
     */
    public void send(String routingKey, OrderEventDTO event) {
        // 发送到订单事件交换机，由 routingKey 决定进入哪个队列。
        rabbitTemplate.convertAndSend(MqEnum.ORDER_EVENT_EXCHANGE.code(), routingKey, event);
    }
}
