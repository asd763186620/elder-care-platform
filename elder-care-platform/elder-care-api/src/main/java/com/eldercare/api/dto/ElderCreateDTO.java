package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 新增老人资料请求 DTO。
 *
 * @param userId                老人账号 ID，可为空；为空时用手机号创建账号。
 * @param phone                 老人手机号。
 * @param elderName             老人姓名。
 * @param age                   年龄。
 * @param address               详细住址。
 * @param healthNote            健康备注。
 * @param emergencyContactName  紧急联系人姓名。
 * @param emergencyContactPhone 紧急联系人手机号。
 */
public record ElderCreateDTO(
        // 老人已有账号时传 userId。
        Long userId,
        // 老人手机号，用于创建或关联账号。
        String phone,
        // 老人姓名不能为空。
        @NotBlank(message = "不能为空") String elderName,
        // 老人年龄。
        Integer age,
        // 老人住址。
        String address,
        // 健康情况备注。
        String healthNote,
        // 紧急联系人姓名。
        String emergencyContactName,
        // 紧急联系人手机号。
        String emergencyContactPhone
) {
}
