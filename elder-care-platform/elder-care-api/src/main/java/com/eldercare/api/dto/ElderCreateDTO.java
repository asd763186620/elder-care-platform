package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

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
        @Size(max = 32, message = "不能超过32个字符") String phone,
        // 老人姓名不能为空。
        @NotBlank(message = "不能为空") @Size(max = 64, message = "不能超过64个字符") String elderName,
        // 老人年龄。
        @Min(value = 0, message = "不能小于0") @Max(value = 120, message = "不能大于120") Integer age,
        // 老人住址。
        @Size(max = 255, message = "不能超过255个字符") String address,
        // 健康情况备注。
        @Size(max = 512, message = "不能超过512个字符") String healthNote,
        // 紧急联系人姓名。
        @Size(max = 64, message = "不能超过64个字符") String emergencyContactName,
        // 紧急联系人手机号。
        @Size(max = 32, message = "不能超过32个字符") String emergencyContactPhone
) {
}
