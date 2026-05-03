package com.eldercare.api.vo;

import java.util.List;

/**
 * 当前登录用户信息 VO。
 *
 * @param userId      用户 ID。
 * @param communityId 社区 ID。
 * @param phone       手机号。
 * @param nickname    昵称。
 * @param roles       角色列表。
 */
public record UserInfoVO(
        // 用户账号 ID。
        Long userId,
        // 当前社区 ID。
        Long communityId,
        // 用户手机号。
        String phone,
        // 用户昵称。
        String nickname,
        // 用户角色列表。
        List<String> roles
) {
}
