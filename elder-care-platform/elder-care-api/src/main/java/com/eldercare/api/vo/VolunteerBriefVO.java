package com.eldercare.api.vo;

/**
 * 志愿者简要信息 VO。
 *
 * @param volunteerId 志愿者 ID。
 * @param name        志愿者姓名。
 * @param phone       志愿者手机号。
 */
public record VolunteerBriefVO(
        // 志愿者用户 ID。
        Long volunteerId,
        // 志愿者展示姓名。
        String name,
        // 志愿者联系电话。
        String phone
) {
}
