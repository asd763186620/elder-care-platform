package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 微信小程序登录请求。
 *
 * @param code        小程序 wx.login 返回的临时登录凭证。
 * @param communityId 当前选择的社区 ID，不传时使用演示社区。
 * @param loginRole   本次希望启用的角色，账号必须已经拥有该角色。
 * @param roles       兼容第一版 mock 调试的角色列表；正式小程序推荐使用 loginRole。
 */
public record MiniAppLoginDTO(
        // code 是调用微信 code2session 的必要参数。
        @NotBlank(message = "code不能为空") String code,
        // 社区 ID 用于社区级数据隔离。
        Long communityId,
        // 当前登录角色；多身份账号用它选择当前身份。
        String loginRole,
        // 多身份账号允许一次登录带多个角色。
        List<String> roles
) {
}
