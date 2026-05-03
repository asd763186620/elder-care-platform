package com.eldercare.common.aspect;

import com.eldercare.common.annotation.RequireRole;
import com.eldercare.common.context.UserContext;
import com.eldercare.common.context.UserInfoDTO;
import com.eldercare.common.exception.BizException;
import com.eldercare.common.exception.ErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.Arrays;

/**
 * 角色权限校验切面。
 * 说明：方法级 @RequireRole 优先于类级 @RequireRole，支持一个接口允许多个角色访问。
 */
@Aspect
public class RequireRoleAspect {

    /**
     * 拦截方法级 @RequireRole。
     *
     * @param requireRole 方法上的角色要求。
     */
    @Before("@annotation(requireRole)")
    public void checkMethodRole(RequireRole requireRole) {
        // 校验当前用户是否拥有目标角色。
        check(requireRole);
    }

    /**
     * 拦截类级 @RequireRole，但排除已经声明方法级 @RequireRole 的方法。
     *
     * @param requireRole 类上的角色要求。
     */
    @Before("@within(requireRole) && !@annotation(com.eldercare.common.annotation.RequireRole)")
    public void checkClassRole(RequireRole requireRole) {
        // 校验当前用户是否拥有目标角色。
        check(requireRole);
    }

    /**
     * 执行角色校验。
     *
     * @param requireRole 注解配置。
     */
    private void check(RequireRole requireRole) {
        // 从请求头加载当前用户上下文。
        UserInfoDTO userInfo = UserContext.loadFromCurrentRequest();
        // 用户不存在时返回未登录。
        if (userInfo == null || userInfo.userId() == null) {
            // 抛出 401 异常。
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        // 判断当前用户是否拥有允许角色中的任意一个。
        boolean matched = Arrays.stream(requireRole.value()).anyMatch(userInfo::hasRole);
        // 不匹配时返回无权限。
        if (!matched) {
            // 抛出 403 异常。
            throw new BizException(ErrorCode.FORBIDDEN, "当前角色无权访问该接口");
        }
    }
}
