package com.eldercare.community.enums;

/**
 * 社区和服务项目状态枚举。
 */
public enum CommunityStatusEnum {
    NORMAL(1, "正常");

    private final Integer code;
    private final String message;

    CommunityStatusEnum(Integer code, String message) {
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
