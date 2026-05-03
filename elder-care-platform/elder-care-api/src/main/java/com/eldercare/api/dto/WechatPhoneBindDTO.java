package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信手机号绑定请求。
 *
 * @param code 小程序 getPhoneNumber 返回的手机号凭证 code。
 */
public record WechatPhoneBindDTO(
        // 该 code 需要由后端调用微信接口换取可信手机号。
        @NotBlank(message = "手机号凭证code不能为空") String code
) {
}
