package com.eldercare.user.enums;

/**
 * 亲情号绑定来源枚举。
 */
public enum FamilyBindSourceEnum {
    MINI_APP("MINI_APP", "小程序");

    private final String code;
    private final String message;

    FamilyBindSourceEnum(String code, String message) {
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
