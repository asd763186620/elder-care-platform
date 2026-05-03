package com.eldercare.common.context;

import java.util.List;

/**
 * 当前登录用户上下文 DTO。
 *
 * @param userId      当前登录用户 ID。
 * @param communityId 当前用户所在社区 ID。
 * @param roles       当前用户拥有的角色列表。
 */
public record UserInfoDTO(Long userId, Long communityId, List<String> roles) {

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * @param roleCode 角色编码。
     * @return true 表示拥有该角色。
     */
    public boolean hasRole(String roleCode) {
        // roles 为空时直接返回 false。
        if (roles == null || roles.isEmpty()) {
            // 没有任何角色。
            return false;
        }
        // 判断角色列表中是否包含目标角色。
        return roles.contains(roleCode);
    }
}
