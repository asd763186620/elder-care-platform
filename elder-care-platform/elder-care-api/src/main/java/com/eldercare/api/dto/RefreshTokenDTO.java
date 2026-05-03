package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新 Token 请求。
 *
 * @param refreshToken 登录时签发的刷新令牌。
 */
public record RefreshTokenDTO(
        // 刷新令牌必须由服务端签发。
        @NotBlank(message = "refreshToken不能为空") String refreshToken
) {
}
