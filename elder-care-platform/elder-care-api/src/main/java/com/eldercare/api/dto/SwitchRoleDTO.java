package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 多身份账号切换当前角色请求。
 *
 * @param role        目标角色编码，第二版小程序端推荐字段。
 * @param roleCode    兼容第一版字段。
 * @param communityId 目标社区 ID；为空时使用当前 Token 中的社区。
 */
public record SwitchRoleDTO(
        // 目标角色必须是当前账号已经拥有且启用的角色。
        String role,
        // 兼容第一版 roleCode 字段。
        String roleCode,
        // 切换社区时使用，社区级平台会校验账号归属。
        Long communityId
) {
    /**
     * 获取最终角色编码。
     *
     * @return 优先返回 role，其次返回 roleCode。
     */
    public String targetRole() {
        // 第二版优先使用更直观的 role 字段。
        return role != null && !role.isBlank() ? role : roleCode;
    }
}
