package com.eldercare.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 多身份账号切换当前角色请求。
 *
 * @param roleCode 目标角色编码。
 */
public record SwitchRoleDTO(
        // 目标角色必须是当前账号已经拥有且启用的角色。
        @NotBlank(message = "角色不能为空") String roleCode
) {
}
