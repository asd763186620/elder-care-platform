package com.eldercare.common.context;

import java.util.List;

/**
 * 当前登录用户信息，通常由网关从 JWT 中解析并透传给业务服务。
 *
 * @param userId      用户主键 ID。
 * @param communityId 当前登录社区 ID。
 * @param roles       用户角色列表。
 * @param role        当前主角色，兼容早期单角色代码。
 * @param phone       用户手机号。
 */
public record LoginUser(Long userId, Long communityId, List<String> roles, UserRole role, String phone) {

    /**
     * 兼容早期只有 userId、role、phone 的构造方式。
     *
     * @param userId 用户主键 ID。
     * @param role   当前主角色。
     * @param phone  用户手机号。
     */
    public LoginUser(Long userId, UserRole role, String phone) {
        // 没有社区 ID 时暂用 null，roles 使用单角色列表。
        this(userId, null, role == null ? List.of() : List.of(role.name()), role, phone);
    }

    /**
     * 兼容多角色登录场景的构造方式。
     *
     * @param userId      用户主键 ID。
     * @param communityId 当前登录社区 ID。
     * @param roles       用户角色列表。
     * @param phone       用户手机号。
     */
    public LoginUser(Long userId, Long communityId, List<String> roles, String phone) {
        // 主角色取 roles 的第一个值，无法识别时为 null。
        this(userId, communityId, roles == null ? List.of() : roles, parsePrimaryRole(roles), phone);
    }

    /**
     * 根据角色列表解析主角色。
     *
     * @param roles 角色列表。
     * @return 主角色；无法解析时返回 null。
     */
    private static UserRole parsePrimaryRole(List<String> roles) {
        // 空角色列表没有主角色。
        if (roles == null || roles.isEmpty()) {
            // 返回 null 表示没有主角色。
            return null;
        }
        try {
            // 使用第一个角色作为主角色。
            return UserRole.valueOf(roles.get(0));
        } catch (IllegalArgumentException exception) {
            // 非法角色不让解析过程失败。
            return null;
        }
    }
}
