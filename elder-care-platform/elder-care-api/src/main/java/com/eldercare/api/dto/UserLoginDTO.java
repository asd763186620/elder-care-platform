package com.eldercare.api.dto;

import com.eldercare.common.context.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 用户登录请求 DTO。
 *
 * @param phone       手机号。
 * @param communityId 当前登录社区 ID。
 * @param role        登录角色。
 */
public record UserLoginDTO(
        // 手机号不能为空，第一版用手机号模拟登录。
        @NotBlank(message = "不能为空") String phone,
        // 社区 ID 第一版允许不传，不传时登录接口会使用演示社区 1。
        Long communityId,
        // 登录角色不能为空，区分老人、亲情号和志愿者。
        @NotNull(message = "不能为空") UserRole role
) {
}
