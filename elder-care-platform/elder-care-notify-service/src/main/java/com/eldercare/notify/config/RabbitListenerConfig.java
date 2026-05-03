package com.eldercare.notify.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消费重试配置。
 */
@Configuration
public class RabbitListenerConfig {
    /**
     * RabbitMQ 监听容器配置。
     *
     * @param connectionFactory        连接工厂。
     * @param jacksonMessageConverter JSON 消息转换器。
     * @return 监听容器工厂。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        // 创建简单监听容器工厂。
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // 设置 RabbitMQ 连接工厂。
        factory.setConnectionFactory(connectionFactory);
        // 设置 JSON 消息转换器，自动把消息转成 OrderEventDTO。
        factory.setMessageConverter(jacksonMessageConverter);
        // 消费异常时重新入队，形成第一版基础重试机制。
        factory.setDefaultRequeueRejected(true);
        // 单个消费者一次最多预取 10 条，避免堆积过多未确认消息。
        factory.setPrefetchCount(10);
        // 返回容器工厂。
        return factory;
    }
}
