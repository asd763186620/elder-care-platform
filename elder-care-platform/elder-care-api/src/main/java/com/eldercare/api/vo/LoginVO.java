package com.eldercare.api.vo;

import com.eldercare.common.context.UserRole;

import java.util.List;

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
        String token,
        // 刷新令牌，用于 access token 过期前后换取新 token。
        String refreshToken,
        // 当前账号拥有的角色列表。
        List<String> roles,
        // 当前登录社区 ID。
        Long communityId,
        // 是否已经绑定手机号。
        Boolean phoneBound
) {
    /**
     * 兼容旧登录代码的简化构造方法。
     *
     * @param userId 用户 ID。
     * @param role   当前角色。
     * @param token  访问令牌。
     */
    public LoginVO(Long userId, UserRole role, String token) {
        // 旧接口没有刷新令牌和手机号状态时使用空值。
        this(userId, role, token, null, role == null ? List.of() : List.of(role.name()), null, null);
    }
}
