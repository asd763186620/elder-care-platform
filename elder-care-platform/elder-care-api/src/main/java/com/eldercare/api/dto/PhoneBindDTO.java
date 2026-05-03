package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 绑定手机号请求。
 *
 * @param phone 手机号；生产环境应由微信手机号凭证换取后端可信手机号。
 */
public record PhoneBindDTO(
        // 第一版先接收明文手机号，后续替换为微信 getPhoneNumber code。
        @NotBlank(message = "手机号不能为空") String phone
) {
}
