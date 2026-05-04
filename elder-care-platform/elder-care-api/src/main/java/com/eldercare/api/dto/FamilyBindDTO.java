package com.eldercare.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 亲情号绑定老人请求 DTO。
 *
 * @param elderUserId  老人用户 ID。
 * @param relationship 亲属关系。
 */
public record FamilyBindDTO(
        // 被绑定的老人用户 ID。
        @NotNull(message = "不能为空") Long elderUserId,
        // 关系，例如 CHILD、SPOUSE、RELATIVE、OTHER。
        @Size(max = 32, message = "不能超过32个字符") String relationship
) {
}
