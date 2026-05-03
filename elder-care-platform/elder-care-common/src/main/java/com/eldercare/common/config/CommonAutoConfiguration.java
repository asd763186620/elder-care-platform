package com.eldercare.common.config;

import com.eldercare.common.aspect.RepeatSubmitAspect;
import com.eldercare.common.aspect.RequireRoleAspect;
import com.eldercare.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * common 模块自动配置。
 * 说明：业务服务只要依赖 elder-care-common，就能自动获得全局异常处理、角色切面和防重复提交切面。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonAutoConfiguration {

    /**
     * 注册全局异常处理器。
     *
     * @return 全局异常处理器。
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        // 返回 common 中统一的异常处理器。
        return new GlobalExceptionHandler();
    }

    /**
     * 注册角色权限切面。
     *
     * @return 角色权限切面。
     */
    @Bean
    @ConditionalOnMissingBean
    public RequireRoleAspect requireRoleAspect() {
        // 返回角色权限切面实例。
        return new RequireRoleAspect();
    }

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
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnMissingBean
    public RepeatSubmitAspect repeatSubmitAspect(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        // 返回防重复提交切面实例。
        return new RepeatSubmitAspect(redisTemplate, objectMapper);
    }
}
