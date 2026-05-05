package com.eldercare.common.enums;

/**
 * 网关和后端服务之间透传的请求头枚举。
 */
public enum HeaderEnum {
    USER_ID("X-User-Id", "用户 ID 请求头"),
    COMMUNITY_ID("X-Community-Id", "社区 ID 请求头"),
    USER_ROLE("X-User-Role", "单角色请求头"),
    ROLES("X-Roles", "多角色请求头"),
    USER_PHONE("X-User-Phone", "用户手机号请求头"),
    TRACE_ID("X-Trace-Id", "请求链路 ID 请求头");

    private final String code;
    private final String message;

    HeaderEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
