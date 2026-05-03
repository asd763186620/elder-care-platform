package com.eldercare.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解。
 * 说明：标记在 Controller 方法上后，RepeatSubmitAspect 会基于 Redis 做幂等窗口控制。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /**
     * 重复提交锁定时间，单位秒。
     * 说明：同一用户、同一 URI、同一请求内容在该时间内只能提交一次。
     *
     * @return Redis Key 的过期秒数。
     */
    long expireSeconds() default 5;

    /**
     * 触发重复提交时返回的提示文案。
     *
     * @return 重复提交提示。
     */
    String message() default "请勿重复提交";
}
