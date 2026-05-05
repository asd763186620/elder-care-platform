package com.eldercare.user.enums;

/**
 * 用户账号和档案状态枚举。
 */
public enum UserAccountStatusEnum {
    NORMAL(1, "正常"),
    DISABLED(2, "禁用");

    private final Integer code;
    private final String message;

    UserAccountStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer code() {
        return code;
    }

    public String message() {
        return message;
    }
}
