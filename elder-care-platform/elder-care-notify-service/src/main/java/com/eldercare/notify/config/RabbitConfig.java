package com.eldercare.notify.config;

import com.eldercare.common.enums.MqEnum;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础配置。
 */
@Configuration
public class RabbitConfig {
    /**
     * 消费端使用 JSON 反序列化订单事件。
     *
     * @return 消息转换器。
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        // 和 order-service 生产端保持一致。
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 声明订单事件交换机。
     */
    @Bean
    public DirectExchange orderEventExchange() {
        // durable=true，保证交换机持久化。
        return new DirectExchange(MqEnum.ORDER_EVENT_EXCHANGE.code(), true, false);
    }

    /**
     * 声明订单通知队列。
     */
    @Bean
    public Queue orderNotifyQueue() {
        // durable 队列可在 RabbitMQ 重启后保留。
        return QueueBuilder.durable(MqEnum.ORDER_NOTIFY_QUEUE.code()).build();
    }

    /**
     * 绑定创建事件。
     */
    @Bean
    public Binding bindCreated(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // 监听 order.created。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqEnum.ORDER_CREATED_ROUTING_KEY.code());
    }

    /**
     * 绑定抢单事件。
     */
    @Bean
    public Binding bindGrabbed(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // 监听 order.grabbed。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqEnum.ORDER_GRABBED_ROUTING_KEY.code());
    }

    /**
     * 绑定取消事件。
     */
    @Bean
    public Binding bindCancelled(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // 监听 order.cancelled。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqEnum.ORDER_CANCELLED_ROUTING_KEY.code());
    }

    /**
     * 绑定完成事件。
     */
    @Bean
    public Binding bindCompleted(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // 监听 order.completed。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqEnum.ORDER_COMPLETED_ROUTING_KEY.code());
    }

    /**
     * 绑定开始服务事件。
     */
    @Bean
    public Binding bindStarted(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // 监听 order.started。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqEnum.ORDER_STARTED_ROUTING_KEY.code());
    }

    /**
     * 绑定提交完成事件。
     */
    @Bean
    public Binding bindSubmitted(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // 监听 order.submitted。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqEnum.ORDER_SUBMITTED_ROUTING_KEY.code());
    }
}
