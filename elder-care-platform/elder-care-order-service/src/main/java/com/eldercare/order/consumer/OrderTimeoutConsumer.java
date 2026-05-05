package com.eldercare.order.consumer;

import com.eldercare.common.enums.MqEnum;
import com.eldercare.order.message.OrderTimeoutMessage;
import com.eldercare.order.service.OrderAppService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时死信消费者。
 */
@Component
public class OrderTimeoutConsumer {
    /** 订单业务服务。 */
    private final OrderAppService orderAppService;

    public OrderTimeoutConsumer(OrderAppService orderAppService) {
        // 保存订单业务服务。
        this.orderAppService = orderAppService;
    }

    /**
     * 消费超时死信消息。
     */
    @RabbitListener(queues = "#{T(com.eldercare.common.enums.MqEnum).ORDER_TIMEOUT_DEAD_QUEUE.code()}")
    public void consume(OrderTimeoutMessage message) {
        // 消息过期后只尝试关闭该订单；如果订单已被抢或已取消，业务方法会幂等跳过。
        orderAppService.closeTimeoutOrder(message.orderId(), message.communityId());
    }
}
