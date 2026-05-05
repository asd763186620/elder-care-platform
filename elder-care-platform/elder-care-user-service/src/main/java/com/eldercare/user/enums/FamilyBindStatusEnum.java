package com.eldercare.user.enums;

/**
 * 亲情号绑定状态枚举。
 */
public enum FamilyBindStatusEnum {
    BOUND(2, "已绑定");

    private final Integer code;
    private final String message;

    FamilyBindStatusEnum(Integer code, String message) {
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
