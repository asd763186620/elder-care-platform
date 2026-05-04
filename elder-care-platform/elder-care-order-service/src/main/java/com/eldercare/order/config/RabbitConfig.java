package com.eldercare.order.config;

import com.eldercare.common.constant.MqConstants;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单事件 RabbitMQ 配置。
 */
@Configuration
public class RabbitConfig {
    /**
     * 使用 Jackson JSON 序列化消息，避免 Java 原生序列化导致跨服务兼容性差。
     *
     * @return JSON 消息转换器。
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        // RabbitTemplate 会用该转换器把 OrderEventDTO 转成 JSON。
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制 RabbitTemplate，使生产者发送 JSON 消息。
     *
     * @param connectionFactory        RabbitMQ 连接工厂。
     * @param jacksonMessageConverter JSON 消息转换器。
     * @return RabbitTemplate。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        // 创建 RabbitTemplate。
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 设置 JSON 消息转换器。
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        // 返回定制后的 RabbitTemplate。
        return rabbitTemplate;
    }

    /**
     * 声明订单事件交换机。
     *
     * @return DirectExchange。
     */
    @Bean
    public DirectExchange orderEventExchange() {
        // durable=true 表示交换机持久化，autoDelete=false 表示不用时不自动删除。
        return new DirectExchange(MqConstants.ORDER_EVENT_EXCHANGE, true, false);
    }

    /**
     * 声明订单通知队列。
     *
     * @return Queue。
     */
    @Bean
    public Queue orderNotifyQueue() {
        // durable 队列可在 RabbitMQ 重启后保留。
        return QueueBuilder.durable(MqConstants.ORDER_NOTIFY_QUEUE).build();
    }

    /**
     * 绑定订单创建事件。
     */
    @Bean
    public Binding bindCreated(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // order.created 路由到通知队列。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqConstants.ORDER_CREATED_ROUTING_KEY);
    }

    /**
     * 绑定订单抢单成功事件。
     */
    @Bean
    public Binding bindGrabbed(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // order.grabbed 路由到通知队列。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqConstants.ORDER_GRABBED_ROUTING_KEY);
    }

    /**
     * 绑定订单取消事件。
     */
    @Bean
    public Binding bindCancelled(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // order.cancelled 路由到通知队列。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqConstants.ORDER_CANCELLED_ROUTING_KEY);
    }

    /**
     * 绑定订单完成事件。
     */
    @Bean
    public Binding bindCompleted(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // order.completed 路由到通知队列。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqConstants.ORDER_COMPLETED_ROUTING_KEY);
    }

    /**
     * 绑定订单开始服务事件。
     */
    @Bean
    public Binding bindStarted(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // order.started 路由到通知队列。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqConstants.ORDER_STARTED_ROUTING_KEY);
    }

    /**
     * 绑定订单提交完成事件。
     */
    @Bean
    public Binding bindSubmitted(Queue orderNotifyQueue, DirectExchange orderEventExchange) {
        // order.submitted 路由到通知队列。
        return BindingBuilder.bind(orderNotifyQueue).to(orderEventExchange).with(MqConstants.ORDER_SUBMITTED_ROUTING_KEY);
    }
}
