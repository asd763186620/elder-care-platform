package com.eldercare.api.vo;

/**
 * 老人资料 VO。
 *
 * @param elderProfileId 老人档案 ID。
 * @param elderUserId    老人用户 ID。
 * @param elderName      老人姓名。
 * @param elderPhone     老人手机号。
 * @param age            年龄。
 * @param address        详细住址。
 * @param healthNote     健康备注。
 */
public record ElderProfileVO(
        // 老人档案 ID。
        Long elderProfileId,
        // 老人用户 ID。
        Long elderUserId,
        // 老人姓名。
        String elderName,
        // 老人手机号。
        String elderPhone,
        // 老人年龄。
        Integer age,
        // 老人住址。
        String address,
        // 健康备注。
        String healthNote
) {
}
