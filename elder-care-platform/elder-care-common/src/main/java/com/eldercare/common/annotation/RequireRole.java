package com.eldercare.common.annotation;

import com.eldercare.common.enums.RoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限校验注解。
 * 说明：标记在 Controller 类或方法上后，RequireRoleAspect 会校验当前登录用户是否拥有任意一个要求角色。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色。
     * 说明：满足其中任意一个角色即可访问。
     *
     * @return 允许访问的角色列表。
     */
    RoleEnum[] value();
}
