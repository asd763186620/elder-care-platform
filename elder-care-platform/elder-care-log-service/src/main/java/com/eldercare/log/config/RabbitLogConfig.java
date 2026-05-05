package com.eldercare.log.config;

import com.eldercare.common.enums.MqEnum;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 日志服务 RabbitMQ 配置。
 */
@Configuration
public class RabbitLogConfig {
    /** JSON 消息转换器。 */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        // 与生产者保持 JSON 格式一致。
        return new Jackson2JsonMessageConverter();
    }

    /** 声明日志交换机。 */
    @Bean
    public DirectExchange logExchange() {
        // durable=true 保证交换机持久化。
        return new DirectExchange(MqEnum.LOG_EXCHANGE.code(), true, false);
    }

    /** 接口访问日志队列。 */
    @Bean
    public Queue apiAccessLogQueue() {
        // durable 队列可在 RabbitMQ 重启后保留。
        return QueueBuilder.durable(MqEnum.API_ACCESS_LOG_QUEUE.code()).build();
    }

    /** 操作日志队列。 */
    @Bean
    public Queue operationLogQueue() {
        // durable 队列可在 RabbitMQ 重启后保留。
        return QueueBuilder.durable(MqEnum.OPERATION_LOG_QUEUE.code()).build();
    }

    /** 绑定接口访问日志队列。 */
    @Bean
    public Binding apiAccessLogBinding(Queue apiAccessLogQueue, DirectExchange logExchange) {
        // 绑定接口访问日志 routingKey。
        return BindingBuilder.bind(apiAccessLogQueue).to(logExchange).with(MqEnum.API_ACCESS_LOG_ROUTING_KEY.code());
    }

    /** 绑定操作日志队列。 */
    @Bean
    public Binding operationLogBinding(Queue operationLogQueue, DirectExchange logExchange) {
        // 绑定操作日志 routingKey。
        return BindingBuilder.bind(operationLogQueue).to(logExchange).with(MqEnum.OPERATION_LOG_ROUTING_KEY.code());
    }
}
