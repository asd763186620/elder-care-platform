package com.eldercare.common.config;

import com.eldercare.common.aspect.RepeatSubmitAspect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 防重复提交自动配置。
 * 说明：该配置独立拆分，避免不使用 Redis 的服务在反射 CommonAutoConfiguration 时加载 Redis 类失败。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
public class RepeatSubmitAutoConfiguration {

    /**
     * 注册防重复提交切面。
     * 说明：只有服务中存在 RedisTemplate 时才创建，避免不使用 Redis 的服务启动失败。
     *
     * @param redisTemplate Redis 字符串客户端。
     * @param objectMapper  Jackson 序列化工具。
     * @return 防重复提交切面。
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean
    public RepeatSubmitAspect repeatSubmitAspect(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        // 返回防重复提交切面实例。
        return new RepeatSubmitAspect(redisTemplate, objectMapper);
    }
}
