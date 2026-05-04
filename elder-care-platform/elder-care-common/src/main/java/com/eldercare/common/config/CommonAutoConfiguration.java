package com.eldercare.common.config;

import com.eldercare.common.aspect.RequireRoleAspect;
import com.eldercare.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

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
}
