package com.eldercare.common.log.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 日志 RabbitMQ 通用配置。
 * 说明：所有业务服务和网关发送日志消息时统一使用 JSON，避免 Java 原生序列化跨服务不兼容。
 */
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class LogRabbitConfig {

    /**
     * 没有业务自定义 MessageConverter 时，提供一个 Jackson JSON 转换器。
     */
    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter logJacksonMessageConverter() {
        // Jackson2JsonMessageConverter 会把 DTO 转成 JSON，并携带类型头。
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制自动创建的 RabbitTemplate，使日志消息统一按 JSON 发送。
     */
    @Bean
    public RabbitTemplateCustomizer logRabbitTemplateCustomizer(MessageConverter messageConverter) {
        // 返回自定义器，由 Spring Boot 在创建 RabbitTemplate 后调用。
        return rabbitTemplate -> rabbitTemplate.setMessageConverter(messageConverter);
    }
}
