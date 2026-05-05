package com.eldercare.auth.enums;

/**
 * 认证服务业务状态枚举。
 */
public enum AuthStatusEnum {
    DEFAULT_COMMUNITY(1, "默认演示社区"),
    ACCOUNT_NORMAL(1, "账号正常"),
    ACCOUNT_DISABLED(2, "账号禁用");

    private final Integer code;
    private final String message;

    AuthStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer code() {
        return code;
    }

    public Long longCode() {
        return code.longValue();
    }

    public String message() {
        return message;
    }
}
