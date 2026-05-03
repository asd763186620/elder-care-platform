package com.eldercare.api.vo;

import com.eldercare.common.context.UserRole;

/**
 * 登录响应 VO。
 *
 * @param userId 用户 ID。
 * @param role   用户角色。
 * @param token  JWT 登录令牌。
 */
public record LoginVO(
        // 当前登录用户 ID。
        Long userId,
        // 当前登录用户角色。
        UserRole role,
        // 前端后续请求需要携带的 JWT。
        String token
) {
}
