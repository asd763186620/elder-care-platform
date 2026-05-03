package com.eldercare.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 模拟登录请求 DTO。
 *
 * @param userId      用户 ID，和 phone 二选一。
 * @param phone       手机号，和 userId 二选一。
 * @param communityId 社区 ID，不传时后端使用演示社区 1。
 * @param roles       登录角色列表，不传时默认老人角色。
 */
public record MockLoginDTO(
        // 已存在用户可以直接使用 userId 模拟登录。
        Long userId,
        // 新用户或不知道 userId 时可以用手机号模拟登录。
        String phone,
        // 社区 ID 用于生成 JWT 中的 communityId。
        Long communityId,
        // 多身份登录角色列表，例如 ELDER,FAMILY,VOLUNTEER。
        List<String> roles
) {
}
