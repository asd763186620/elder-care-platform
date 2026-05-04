package com.eldercare.api.vo;

import java.util.List;

/**
 * 当前登录用户详情。
 *
 * @param userId           用户 ID。
 * @param nickname         昵称。
 * @param phone            手机号。
 * @param avatar           头像地址。
 * @param currentRole      当前启用角色。
 * @param communityId      当前社区 ID。
 * @param roleList         账号拥有的角色列表。
 * @param elderProfile     老人档案摘要。
 * @param volunteerProfile 志愿者档案摘要。
 */
public record AuthCurrentVO(
        // 当前登录用户 ID。
        Long userId,
        // 用户昵称。
        String nickname,
        // 用户手机号，未绑定时为空。
        String phone,
        // 用户头像地址。
        String avatar,
        // 当前启用角色。
        String currentRole,
        // 当前社区 ID。
        Long communityId,
        // 账号角色列表。
        List<String> roleList,
        // 老人档案摘要，非老人或未建档时为空。
        ElderProfileVO elderProfile,
        // 志愿者档案摘要，非志愿者或未完善资料时为空。
        VolunteerBriefVO volunteerProfile
) {
}
